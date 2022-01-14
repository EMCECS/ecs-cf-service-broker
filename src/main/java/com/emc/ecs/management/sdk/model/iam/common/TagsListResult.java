package com.emc.ecs.management.sdk.model.iam.common;

import com.emc.ecs.management.sdk.model.iam.TagResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@XmlAccessorType(value = XmlAccessType.PROPERTY)
public class TagsListResult extends AbstractIamPagedEntity {

    private List<TagResponse> tags;

    @XmlElementWrapper(name = "Tags")
    @XmlElement(name = "member")
    @JsonProperty("Tags")
    public List<TagResponse> getTags() {
        return tags;
    }

    public void setTags(List<TagResponse> tags) {
        this.tags = tags;
    }
}
