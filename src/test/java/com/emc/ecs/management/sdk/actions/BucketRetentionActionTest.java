package com.emc.ecs.management.sdk.actions;

import com.emc.ecs.management.sdk.actions.BucketAction;
import com.emc.ecs.management.sdk.actions.BucketRetentionAction;
import com.emc.ecs.servicebroker.exception.EcsManagementClientException;
import com.emc.ecs.servicebroker.exception.EcsManagementResourceNotFoundException;
import com.emc.ecs.common.EcsActionTest;
import com.emc.ecs.management.sdk.model.DefaultBucketRetention;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BucketRetentionActionTest extends EcsActionTest {

    private final String BUCKET = "testbucket5";

    @Before
    public void setUp() throws EcsManagementClientException,
            EcsManagementResourceNotFoundException {
        BucketAction.create(connection, BUCKET, namespace, replicationGroupID);
    }

    @After
    public void cleanup() throws EcsManagementClientException {
        BucketAction.delete(connection, BUCKET, namespace);
    }

    @Test
    public void getUpdateBucketRetention() throws Exception {
        DefaultBucketRetention retention =
                BucketRetentionAction.get(connection, namespace, BUCKET);
        assertEquals(-1, retention.getPeriod());
        int THIRTY_DAYS_IN_SEC = 2592000;
        BucketRetentionAction.update(connection, namespace, BUCKET,
                THIRTY_DAYS_IN_SEC);
        DefaultBucketRetention retention2 =
                BucketRetentionAction.get(connection, namespace, BUCKET);
        assertEquals(THIRTY_DAYS_IN_SEC, retention2.getPeriod());
    }
}