package com.emc.ecs.management.sdk.model.iam.user;

import com.emc.ecs.management.sdk.model.iam.BaseIamResponse;
import com.emc.ecs.management.sdk.model.iam.IamResponseConstants;
import com.fasterxml.jackson.annotation.JsonRootName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DeleteUserPermissionsBoundaryResponse", namespace = IamResponseConstants.RESPONSE_XML_NAMESPACE)
@XmlAccessorType(value = XmlAccessType.PROPERTY)
@JsonRootName("DeleteUserPermissionsBoundaryResponse")
public class DeleteUserPermissionsBoundaryResponse extends BaseIamResponse {
}
