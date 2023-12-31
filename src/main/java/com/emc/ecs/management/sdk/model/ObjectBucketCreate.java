package com.emc.ecs.management.sdk.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

import static com.emc.ecs.servicebroker.model.Constants.*;

@XmlRootElement(name = "object_bucket_create")
public class ObjectBucketCreate {

    private Boolean filesystemEnabled = false;
    private Boolean isStaleAllowed = false;
    private Boolean isTsoReadOnly = false;
    private String headType = HEAD_TYPE_S3;
    private String name;
    private String vpool;
    private String namespace;
    private Boolean isEncryptionEnabled;
    private List<SearchMetadata> searchMetadataList;

    public ObjectBucketCreate(String name, String namespace, String replicationGroup, Map<String, Object> params) {
        super();
        this.name = name;
        this.namespace = namespace;
        this.vpool = replicationGroup;
        this.isEncryptionEnabled = (Boolean) params.get(ENCRYPTED);
        this.filesystemEnabled = (Boolean) params.get(FILE_ACCESSIBLE);
        this.isStaleAllowed = (Boolean) params.get(ACCESS_DURING_OUTAGE);
        this.isTsoReadOnly = (Boolean) params.get(ADO_READ_ONLY);
        this.headType = (String) params.getOrDefault(HEAD_TYPE, HEAD_TYPE_S3);
        this.searchMetadataList = (List<SearchMetadata>) params.get(SEARCH_METADATA);
    }

    public ObjectBucketCreate(String name, String namespace,
                              String replicationGroup) {
        super();
        this.name = name;
        this.namespace = namespace;
        this.vpool = replicationGroup;
    }

    public ObjectBucketCreate() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVpool() {
        return vpool;
    }

    public void setVpool(String replicationGroup) {
        this.vpool = replicationGroup;
    }

    @XmlElement(name = "filesystem_enabled")
    public Boolean getFilesystemEnabled() {
        return filesystemEnabled;
    }

    public void setFilesystemEnabled(Boolean filesystemEnabled) {
        this.filesystemEnabled = filesystemEnabled;
    }

    @XmlElement(name = "head_type")
    public String getHeadType() {
        return headType;
    }

    public void setHeadType(String headType) {
        this.headType = headType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @XmlElement(name = "is_stale_allowed")
    public Boolean getIsStaleAllowed() {
        return isStaleAllowed;
    }

    public void setIsStaleAllowed(Boolean isStaleAllowed) {
        this.isStaleAllowed = isStaleAllowed;
    }

    @XmlElement(name = "is_tso_read_only")
    public Boolean getIsTsoReadOnly() {
        return isTsoReadOnly;
    }

    public void setIsTsoReadOnly(Boolean isTsoReadOnly) {
        this.isTsoReadOnly = isTsoReadOnly;
    }

    @XmlElement(name = "is_encryption_enabled")
    public Boolean getIsEncryptionEnabled() {
        return isEncryptionEnabled;
    }

    public void setIsEncryptionEnabled(Boolean isEncryptionEnabled) {
        this.isEncryptionEnabled = isEncryptionEnabled;
    }

    @XmlElementWrapper(name = "search_metadata")
    @XmlElement(name = "metadata")
    public List<SearchMetadata> getSearchMetadataList() {
        return searchMetadataList;
    }

    public void setSearchMetadataList(List<SearchMetadata> searchMetadataList) {
        this.searchMetadataList = searchMetadataList;
    }
}