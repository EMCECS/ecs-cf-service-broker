package com.emc.ecs.management.sdk.actions.iam;

import com.emc.ecs.management.sdk.ManagementAPIConnection;
import com.emc.ecs.management.sdk.model.iam.policy.CreatePolicyResponse;
import com.emc.ecs.management.sdk.model.iam.policy.GetPolicyResponse;
import com.emc.ecs.management.sdk.model.iam.policy.IamPolicy;
import com.emc.ecs.servicebroker.exception.EcsManagementClientException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import static com.emc.ecs.management.sdk.ManagementAPIConstants.IAM;
import static com.emc.ecs.management.sdk.actions.iam.IAMActionUtils.accountHeader;
import static javax.ws.rs.HttpMethod.POST;

public class IAMPolicyAction {
    public static IamPolicy create(ManagementAPIConnection connection, String policyName, String policyDocument, String accountId) throws EcsManagementClientException {
        String encodedDocument = null;
        try {
            encodedDocument = URLEncoder.encode(policyDocument, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        UriBuilder uri = connection.uriBuilder()
                .segment(IAM)
                .queryParam("Action", "CreatePolicy")
                .queryParam("PolicyDocument", encodedDocument)
                .queryParam("PolicyName", policyName);

        Response response = IAMActionUtils.remoteCall(connection, POST, uri, null, accountHeader(accountId));

        CreatePolicyResponse ret = response.readEntity(CreatePolicyResponse.class);
        return ret.getCreatePolicyResult().getPolicy();
    }

    public static IamPolicy get(ManagementAPIConnection connection, String policyARN, String accountId) throws EcsManagementClientException {
        UriBuilder uri = connection.uriBuilder()
                .segment(IAM)
                .queryParam("Action", "GetPolicy")
                .queryParam("PolicyArn", policyARN);

        Response response = IAMActionUtils.remoteCall(connection, POST, uri, null, accountHeader(accountId));

        GetPolicyResponse ret = response.readEntity(GetPolicyResponse.class);
        return ret.getGetPolicyResult().getPolicy();
    }

    public static void delete(ManagementAPIConnection connection, String policyARN, String accountId) throws EcsManagementClientException {
        UriBuilder uri = connection.uriBuilder()
                .segment(IAM)
                .queryParam("Action", "DeletePolicy")
                .queryParam("PolicyArn", policyARN);

        IAMActionUtils.remoteCall(connection, POST, uri, null, accountHeader(accountId));
    }
}
