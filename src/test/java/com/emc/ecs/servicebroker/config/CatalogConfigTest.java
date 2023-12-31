package com.emc.ecs.servicebroker.config;

import com.emc.ecs.common.Fixtures;

import com.emc.ecs.servicebroker.service.EcsServiceTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static com.emc.ecs.common.Fixtures.*;
import static com.emc.ecs.servicebroker.model.Constants.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("test")
public class CatalogConfigTest {

    @Autowired
    private Catalog catalog;

    @Autowired
    private CatalogConfig catalogConfig;

    @Test
    public void testEcsBucket() {
        ServiceDefinition ecsBucketService = catalog.getServiceDefinitions()
                .get(1);
        testServiceDefinition(ecsBucketService,
                "f3cbab6a-5172-4ff1-a5c7-72990f0ce2aa", "ecs-bucket",
                "Elastic Cloud S3 Object Storage Bucket", true, true, Collections.emptyList(), null);
    }

    @Test
    public void testEcsBucketMetadata() {
        ServiceDefinition ecsBucketService = catalog.getServiceDefinitions()
                .get(1);
        Map<String, Object> metadata = ecsBucketService.getMetadata();
        testServiceDefinitionMetadata(metadata, "ecs-bucket",
                "http://www.emc.com/images/products/header-image-icon-ecs.png",
                "Dell EMC", "https://community.emc.com/docs/DOC-45012",
                "http://www.emc.com/products-solutions/trial-software-download/ecs.htm");
    }

    @Test
    public void testEcsBucketPlans() {
        // Service Definition Plans
        ServiceDefinition service = catalog.getServiceDefinitions().get(1);
        List<Plan> ecsBucketPlans = service.getPlans();
        Plan plan0 = ecsBucketPlans.get(0);

        testPlan(plan0, "8e777d49-0a78-4cf4-810a-b5f5173b019d", "5gb",
                "Free Trial", 0.0, "MONTHLY",
                Arrays.asList("Shared object storage", "5 GB Storage",
                        "S3 protocol access"));

        Plan plan1 = ecsBucketPlans.get(1);
        testPlan(plan1, "89d20694-9ab0-4a98-bc6a-868d6d4ecf31", "unlimited",
                "Pay per GB for Month", 0.03, "PER GB PER MONTH",
                Arrays.asList("Shared object storage", "Unlimited Storage",
                        "S3 protocol access"));
    }

    @Test
    public void testBucketTagsValidation() {
        Map<String, Object> settingsWithInvalidCharacters = new HashMap<>();
        settingsWithInvalidCharacters.put(BUCKET_TAGS, BUCKET_TAGS_INVALID_CHARS);

        Map<String, Object> settingsWithInvalidFormat = new HashMap<>();
        settingsWithInvalidFormat.put(BUCKET_TAGS, BUCKET_TAGS_INVALID_FORMAT);

        Map<String, Object> settingsWithLongKey = new HashMap<>();
        settingsWithLongKey.put(BUCKET_TAGS, BUCKET_TAGS_LONG_KEY);

        Map<String, Object> settingsWithLongValue = new HashMap<>();
        settingsWithLongValue.put(BUCKET_TAGS, BUCKET_TAGS_LONG_VALUE);

        assertThrows(ServiceBrokerException.class, () -> CatalogConfig.parseBucketTags(settingsWithInvalidCharacters));
        assertThrows(ServiceBrokerException.class, () -> CatalogConfig.parseBucketTags(settingsWithInvalidFormat));
        assertThrows(ServiceBrokerException.class, () -> CatalogConfig.parseBucketTags(settingsWithLongKey));
        assertThrows(ServiceBrokerException.class, () -> CatalogConfig.parseBucketTags(settingsWithLongValue));
    }

    @Test
    public void parseBucketTagsTest() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(BUCKET_TAGS, Fixtures.BUCKET_TAGS_STRING);
        settings = CatalogConfig.parseBucketTags(settings);

        List<Map<String, String>> resultTags = (List<Map<String, String>>)settings.get(TAGS);
        List<Map<String, String>> expectedTags = Fixtures.listOfBucketTagsFixture();

        assertTrue(CollectionUtils.isEqualCollection(expectedTags, resultTags));
    }

    @Test
    public void parseSearchMetadataTest() throws JsonProcessingException {
        String searchMetadataString = buildSearchMetadataString(SEARCH_METADATA_TYPE_SYSTEM, SYSTEM_METADATA_NAME, SYSTEM_METADATA_TYPE,
                SEARCH_METADATA_TYPE_USER, USER_METADATA_NAME, USER_METADATA_TYPE);

        List<Map<String, String>> resultMetadata = catalogConfig.parseSearchMetadata(searchMetadataString);
        List<Map<String, String>> expectedMetadata = EcsServiceTest.createListOfSearchMetadata(SEARCH_METADATA_TYPE_SYSTEM, SYSTEM_METADATA_NAME, SYSTEM_METADATA_TYPE,
                SEARCH_METADATA_TYPE_USER, USER_METADATA_NAME, USER_METADATA_TYPE);

        assertTrue(CollectionUtils.isEqualCollection(expectedMetadata, resultMetadata));
    }

    private String buildSearchMetadataString(String... args) {
        if (args.length % 3 != 0) {
            throw new IllegalArgumentException("Number of arguments should be multiple of three.");
        }
        if (args.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < args.length; i += 3) {
            if (i != 0) {
                builder.append(',');
            }
            builder.append('{');
            builder.append('\"' + SEARCH_METADATA_TYPE + "\":\"" + args[i] + "\",");
            builder.append('\"' + SEARCH_METADATA_NAME + "\":\"" + args[i + 1] + "\",");
            builder.append('\"' + SEARCH_METADATA_DATATYPE + "\":\"" + args[i + 2] + '\"');
            builder.append('}');
        }
        builder.append(']');
        return builder.toString();
    }

    private void testServiceDefinition(ServiceDefinition service, String id,
                                       String name, String description, boolean bindable,
                                       boolean updatable, List<String> requires, String dashboardUrl) {
        assertEquals(id, service.getId());
        assertEquals(name, service.getName());
        assertEquals(description, service.getDescription());
        assertEquals(bindable, service.isBindable());
        assertEquals(updatable, service.isPlanUpdateable());
        assertEquals(requires, service.getRequires());
    }

    private void testServiceDefinitionMetadata(Map<String, Object> metadata,
                                               String displayName, String imageUrl, String providerDisplayName,
                                               String documentationUrl, String supportUrl) {
        assertEquals(displayName, metadata.get("displayName"));
        assertEquals(imageUrl, metadata.get("imageUrl"));
        assertEquals(providerDisplayName, metadata.get("providerDisplayName"));
        assertEquals(documentationUrl, metadata.get("documentationUrl"));
        assertEquals(supportUrl, metadata.get("supportUrl"));
    }

    private void testPlan(Plan plan, String id, String name, String description,
                          Double usdCost, String unit, List<String> bullets) {
        assertEquals(id, plan.getId());
        assertEquals(name, plan.getName());
        assertEquals(description, plan.getDescription());

        Map<String, Object> metadata = plan.getMetadata();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> costs = (List<Map<String, Object>>) metadata
                .get("costs");
        Map<String, Object> cost = costs.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> amount = (Map<String, Object>) cost.get("amount");
        assertEquals(usdCost, amount.get("usd"));
        assertEquals(unit, cost.get("unit"));
        assertEquals(bullets, metadata.get("bullets"));
    }

}
