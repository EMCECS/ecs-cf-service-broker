package com.emc.ecs.servicebroker.service;

import com.emc.ecs.management.sdk.ManagementAPIConnection;
import com.emc.ecs.management.sdk.ObjectscaleGatewayConnection;
import com.emc.ecs.management.sdk.actions.*;
import com.emc.ecs.management.sdk.model.*;
import com.emc.ecs.servicebroker.config.BrokerConfig;
import com.emc.ecs.servicebroker.config.CatalogConfig;
import com.emc.ecs.servicebroker.exception.EcsManagementClientException;
import com.emc.ecs.servicebroker.model.PlanProxy;
import com.emc.ecs.servicebroker.model.ReclaimPolicy;
import com.emc.ecs.servicebroker.model.ServiceDefinitionProxy;
import com.emc.ecs.servicebroker.repository.BucketWipeFactory;
import com.emc.ecs.servicebroker.service.s3.BucketExpirationAction;
import com.emc.ecs.tool.BucketWipeOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.emc.ecs.management.sdk.ManagementAPIConstants.OBJECTSCALE;
import static com.emc.ecs.servicebroker.model.Constants.*;
import static com.emc.ecs.servicebroker.model.Constants.EXPIRATION;
import static com.emc.ecs.servicebroker.service.EcsService.mergeParameters;

@Service
public class ObjectscaleService {
    private static final Logger logger = LoggerFactory.getLogger(ObjectscaleService.class);

    @Autowired
    @Qualifier("managementAPI")
    private ManagementAPIConnection connection;

    @Autowired
    private ObjectscaleGatewayConnection objectscaleGateway;

    @Autowired
    private BrokerConfig broker;

    @Autowired
    private CatalogConfig catalog;

    private String objectEndpoint;

    @Autowired
    private BucketWipeFactory bucketWipeFactory;

    private BucketWipeOperations bucketWipe;

    @PostConstruct
    void initialize() {
        if (OBJECTSCALE.equalsIgnoreCase(broker.getApiType())) {
            logger.info("Initializing Objectscale service with management endpoint {}", broker.getManagementEndpoint());

            try {
  //              lookupObjectEndpoint();
                prepareRepository();
/*
                getS3RepositorySecret();
                prepareBucketWipe();
 */
            } catch (EcsManagementClientException e) {
                logger.error("Failed to initialize Objectscale service: {}", e.getMessage());
                throw new ServiceBrokerException(e.getMessage(), e);
            }

        }
    }

    String prefix(String string) {
        return broker.getPrefix() + string;
    }

    private void lookupObjectEndpoint() throws EcsManagementClientException {
        if (broker.getObjectEndpoint() != null) {
            try {
                URL endpointUrl = new URL(broker.getObjectEndpoint());
                objectEndpoint = broker.getObjectEndpoint();
                logger.info("Using object endpoint address from broker configuration: {}, use ssl: {}", objectEndpoint, broker.getUseSsl());
            } catch (MalformedURLException e) {
                throw new EcsManagementClientException("Malformed URL provided as object endpoint: " + broker.getObjectEndpoint());
            }
        } else {
            List<BaseUrl> baseUrlList = BaseUrlAction.list(connection);
            String urlId;

            if (baseUrlList == null || baseUrlList.isEmpty()) {
                throw new ServiceBrokerException("Cannot determine object endpoint url: base URLs list is empty, check ECS server settings");
            } else if (broker.getBaseUrl() != null) {
                urlId = baseUrlList.stream()
                        .filter(b -> broker.getBaseUrl().equals(b.getName()))
                        .findFirst()
                        .orElseThrow(() -> new ServiceBrokerException("Configured ECS Base URL not found: " + broker.getBaseUrl()))
                        .getId();
            } else {
                Optional<BaseUrl> maybeBaseUrl = baseUrlList.stream()
                        .filter(b -> "DefaultBaseUrl".equals(b.getName()))
                        .findAny();
                if (maybeBaseUrl.isPresent()) {
                    urlId = maybeBaseUrl.get().getId();
                } else {
                    urlId = baseUrlList.get(0).getId();
                }
            }

            BaseUrlInfo baseUrl = BaseUrlAction.get(connection, urlId);
            objectEndpoint = baseUrl.getNamespaceUrl(broker.getNamespace(), broker.getUseSsl());

            logger.info("Object Endpoint address from configured base url '{}': {}", baseUrl.getName(), objectEndpoint);

            if (baseUrl.getName() != null && !baseUrl.getName().equals(broker.getBaseUrl())) {
                logger.info("Setting base url name to '{}'", baseUrl.getName());
                broker.setBaseUrl(baseUrl.getName());
            }
        }

        if (broker.getRepositoryEndpoint() == null) {
            broker.setRepositoryEndpoint(objectEndpoint);
        }
    }

    private void prepareRepository() throws EcsManagementClientException {
        String bucketName = broker.getRepositoryBucket();
        String namespace = broker.getAccountId();

        if (!bucketExists(bucketName, namespace)) {
            logger.info("Preparing repository bucket '{}'", prefix(bucketName));

            ServiceDefinitionProxy service;
            try {
                if (broker.getRepositoryServiceId() == null) {
                    service = catalog.getRepositoryServiceDefinition();
                } else {
                    service = catalog.findServiceDefinition(broker.getRepositoryServiceId());
                }

                PlanProxy plan;
                if (broker.getRepositoryPlanId() == null) {
                    plan = service.getRepositoryPlan();
                } else {
                    plan = service.findPlan(broker.getRepositoryPlanId());
                }

                Map<String, Object> parameters = new HashMap<>();
                parameters.put(NAMESPACE, namespace);

                createBucket("repository", bucketName, service, plan, parameters);
            } catch (ServiceBrokerException e) {
                String errorMessage = "Failed to create broker repository bucket: " + e.getMessage();
                logger.error(errorMessage);
                throw new ServiceBrokerException(errorMessage, e);
            }
        }
    }

    public boolean bucketExists(String bucketName, String namespace) throws EcsManagementClientException {
        return BucketAction.exists(connection, prefix(bucketName), namespace);
    }

    Map<String, Object> createBucket(String serviceInstanceId, String bucketName, ServiceDefinitionProxy serviceDefinition,
                                     PlanProxy plan, Map<String, Object> parameters) {
        try {
            parameters = mergeParameters(broker, serviceDefinition, plan, parameters);
            //parameters = validateAndPrepareSearchMetadata(parameters);

            // Validate the reclaim-policy
            if (!ReclaimPolicy.isPolicyAllowed(parameters)) {
                throw new ServiceBrokerInvalidParametersException("Reclaim Policy " + ReclaimPolicy.getReclaimPolicy(parameters) + " is not one of the allowed polices " + ReclaimPolicy.getAllowedReclaimPolicies(parameters));
            }

            // Validate expiration policy
            if (parameters.containsKey(EXPIRATION) && parameters.get(EXPIRATION) != null) {
                if ((boolean) parameters.get(FILE_ACCESSIBLE)) {
                    throw new ServiceBrokerInvalidParametersException("Cannot apply expiration rule to file accessible bucket");
                }
            }

            logger.info("Creating bucket '{}' with service '{}' plan '{}'({}) and params {}", prefix(bucketName), serviceDefinition.getName(), plan.getName(), plan.getId(), parameters);

            String namespace = (String) parameters.get(NAMESPACE);

            if (bucketExists(bucketName, namespace)) {
                throw new ServiceInstanceExistsException(serviceInstanceId, serviceDefinition.getId());
            }

            DataServiceReplicationGroup replicationGroup = lookupReplicationGroup((String) parameters.get(REPLICATION_GROUP));

            if (replicationGroup == null) {
                throw new ServiceBrokerException("Cannot create bucket - replication group '" + parameters.get(REPLICATION_GROUP) + "' not found");
            }

            BucketAction.create(connection, new ObjectBucketCreate(
                    prefix(bucketName),
                    namespace,
                    replicationGroup.getId(),
                    parameters
            ));

            if (parameters.containsKey(QUOTA) && parameters.get(QUOTA) != null) {
                Map<String, Integer> quota = (Map<String, Integer>) parameters.get(QUOTA);
                logger.info("Applying bucket quota on '{}' in '{}': limit {}, warn {}", prefix(bucketName), namespace, quota.get(QUOTA_LIMIT), quota.get(QUOTA_WARN));
                BucketQuotaAction.create(connection, namespace, prefix(bucketName), quota.get(QUOTA_LIMIT), quota.get(QUOTA_WARN));
            }

            if (parameters.containsKey(DEFAULT_RETENTION) && parameters.get(DEFAULT_RETENTION) != null) {
                logger.info("Applying bucket retention policy on '{}' in '{}': {}", bucketName, namespace, parameters.get(DEFAULT_RETENTION));
                BucketRetentionAction.update(connection, namespace, prefix(bucketName), (int) parameters.get(DEFAULT_RETENTION));
            }

            if (parameters.containsKey(TAGS) && parameters.get(TAGS) != null) {
                List<Map<String, String>> bucketTags = (List<Map<String, String>>) parameters.get(TAGS);
                logger.info("Applying bucket tags on '{}': {}", bucketName, bucketTags);
                BucketTagsAction.create(connection, prefix(bucketName), new BucketTagsParamAdd(namespace, bucketTags));
            }

            /* TODO
            if (parameters.containsKey(EXPIRATION) && parameters.get(EXPIRATION) != null) {
                grantUserLifecycleManagementPolicy(prefix(bucketName), namespace, prefix(broker.getRepositoryUser()));
                logger.info("Applying bucket expiration on '{}': {} days", bucketName, parameters.get(EXPIRATION));
                BucketExpirationAction.update(broker, namespace, prefix(bucketName), (int) parameters.get(EXPIRATION), null);
            }
             */
        } catch (Exception e) {
            String errorMessage = String.format("Failed to create bucket '%s': %s", bucketName, e.getMessage());
            logger.error(errorMessage, e);
            throw new ServiceBrokerException(errorMessage, e);
        }
        return parameters;
    }

    public DataServiceReplicationGroup lookupReplicationGroup(String replicationGroup) throws EcsManagementClientException {
        return ReplicationGroupAction.list(connection).stream()
                .filter(r -> replicationGroup != null && r != null
                        && (replicationGroup.equals(r.getName()) || replicationGroup.equals(r.getId()))
                )
                .findFirst()
                .orElseThrow(() -> new ServiceBrokerException("ECS replication group not found: " + replicationGroup));
    }
}
