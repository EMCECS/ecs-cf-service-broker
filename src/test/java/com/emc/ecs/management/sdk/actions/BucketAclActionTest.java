package com.emc.ecs.management.sdk.actions;

import com.emc.ecs.management.sdk.actions.BucketAclAction;
import com.emc.ecs.management.sdk.actions.BucketAction;
import com.emc.ecs.management.sdk.actions.ObjectUserAction;
import com.emc.ecs.servicebroker.exception.EcsManagementClientException;
import com.emc.ecs.servicebroker.exception.EcsManagementResourceNotFoundException;
import com.emc.ecs.common.EcsActionTest;
import com.emc.ecs.management.sdk.model.BucketAcl;
import com.emc.ecs.management.sdk.model.BucketUserAcl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.emc.ecs.servicebroker.model.Constants.FULL_CONTROL;
import static org.junit.Assert.assertTrue;

public class BucketAclActionTest extends EcsActionTest {
    private String bucket = "testbucket4";
    private String user = "testuser3";

    @Before
    public void setUp() throws EcsManagementClientException,
            EcsManagementResourceNotFoundException {
        BucketAction.create(connection, bucket, namespace, replicationGroupID);
        ObjectUserAction.create(connection, user, namespace);
    }

    @After
    public void cleanup() throws EcsManagementClientException {
        ObjectUserAction.delete(connection, user);
        BucketAction.delete(connection, bucket, namespace);
    }

    @Test
    public void testApplyCheckRemoveBucketUserAcl()
            throws EcsManagementClientException,
            EcsManagementResourceNotFoundException {
        BucketAcl acl = BucketAclAction.get(connection, bucket, namespace);
        List<BucketUserAcl> userAcl = acl.getAcl().getUserAccessList();
        userAcl.add(new BucketUserAcl(user, FULL_CONTROL));
        acl.getAcl().setUserAccessList(userAcl);
        BucketAclAction.update(connection, bucket, acl);
        BucketAcl bucketAcl = BucketAclAction.get(connection, bucket,
                namespace);
        long userAclCount = bucketAcl.getAcl().getUserAccessList().stream()
                .filter(userAcl1 -> userAcl1.getUser().equals(user)).count();
        assertTrue(userAclCount == 1);
    }
}
