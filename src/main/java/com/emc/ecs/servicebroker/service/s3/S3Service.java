package com.emc.ecs.servicebroker.service.s3;

import com.emc.ecs.servicebroker.config.BrokerConfig;
import com.emc.object.s3.S3Client;
import com.emc.object.s3.S3Exception;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.ListObjectsRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URISyntaxException;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private BrokerConfig broker;

    @Autowired
    private S3Client s3;

    private String bucket;

    @PostConstruct
    public void initialize() throws URISyntaxException {
        this.bucket = broker.getPrefixedBucketName();

        if (broker.isConfigValidationMode()) {
            logger.info("Skipping S3 service initialization - working in validation mode");
            return;
        }

        this.testS3EndpointAccess();
    }

    private void testS3EndpointAccess() {
        String bucket = this.bucket;

        logger.info("Testing access to S3 endpoint - querying bucket '{}' existence", bucket);

        if (s3.bucketExists(bucket)) {
            logger.info("Test OK. Bucket {} exists", bucket);
            try {
                ListObjectsRequest listRequest = new ListObjectsRequest(bucket).withMaxKeys(5);
                ListObjectsResult listResult = s3.listObjects(listRequest);
                listResult.getObjects().forEach(this::testObjectAccess);
            } catch (S3Exception e) {
                logger.error("Failed to list objects in bucket '{}' - check S3 credentials and bucket ACL!:", bucket, e);
            }
        } else {
            logger.info("Test OK. Bucket {} doesnt exist yet", bucket);
        }
    }

    private void testObjectAccess(S3Object s3Object) {
        logger.debug("Testing access to '{}'", s3Object.getKey());
        try {
            AccessControlList objectAcl = s3.getObjectAcl(this.bucket, s3Object.getKey());
            CanonicalUser owner = objectAcl.getOwner();
            String objectOwner = owner.getDisplayName();
            String userName = broker.getPrefixedUserName();
            if (!userName.equalsIgnoreCase(objectOwner)) {
                String errorMessage = String.format(
                        "S3 Object owners differ in repository, check repository username in broker settings: current username is '%s', found object owner '%s' on '%s'",
                        userName, objectOwner, s3Object.getKey());
                logger.warn(errorMessage);
            }
        } catch (S3Exception e) {
            logger.error("Failed to get object ACL: {}/{}  - check S3 credentials and bucket ACL!:", this.bucket, s3Object.getKey(), e);
        }
    }

    public void putObject(String filename, Object content) {
        s3.putObject(bucket, filename, content, "application/json");
    }

    public GetObjectResult<InputStream> getObject(String filename) {
        return s3.getObject(bucket, filename);
    }

    public void deleteObject(String filename) {
        s3.deleteObject(bucket, filename);
    }

    public ListObjectsResult listObjects() {
        return s3.listObjects(bucket);
    }

    public ListObjectsResult listObjects(String prefix, String marker, int pageSize) {
        ListObjectsRequest request = new ListObjectsRequest(bucket);
        if (marker != null) {
            request.setMarker(marker);
        }
        request.setPrefix(prefix);
        if (pageSize != 0) {
            request.setMaxKeys(pageSize);
        }
        return s3.listObjects(request);
    }

    /**
     * Returns true if versioning status is set to either enabled or suspended for the bucket.
     * If versioning status is not set i.e. null then returns false.
     *
     * @param bucketName Name of the bucket to check versioning status for.
     * @return whether versioning is enabled/suspended on the given bucket.
     */
    public boolean isBucketVersioningEnabled(String bucketName) {
        return Objects.nonNull(s3.getBucketVersioning(bucketName).getStatus());
    }

}
