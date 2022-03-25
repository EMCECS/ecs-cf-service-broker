package com.emc.ecs.management.sdk.model.iam.policy;

import com.emc.ecs.management.sdk.model.iam.IamResponseConstants;
import com.emc.ecs.management.sdk.model.iam.BaseIamResponse;
import com.emc.ecs.management.sdk.model.iam.ResponseMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * CreatePolicyResponse
 */

@XmlRootElement(name = "CreatePolicyResponse", namespace = IamResponseConstants.RESPONSE_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class CreatePolicyResponse extends BaseIamResponse {
    
    @JsonProperty(value="CreatePolicyResult")
    @XmlElement(name = "CreatePolicyResult")
    private CreatePolicyResult createPolicyResult = null;

    public CreatePolicyResponse createPoicyResult(CreatePolicyResult createPolicyResult) {
        this.createPolicyResult = createPolicyResult;
        return this;
    }

    /**
      * Get createPolicyResult
      * @return createPolicyResult
      */

    public CreatePolicyResult getCreatePolicyResult() {
        return createPolicyResult;
    }

    public void setCreatePolicyResult(CreatePolicyResult createPolicyResult) {
        this.createPolicyResult = createPolicyResult;
    }

    public CreatePolicyResponse responseMetadata(ResponseMetadata responseMetadata) {
        this.setResponseMetadata(responseMetadata);
        return this;
    }

}

