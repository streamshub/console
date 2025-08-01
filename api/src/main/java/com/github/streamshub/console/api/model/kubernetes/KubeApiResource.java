package com.github.streamshub.console.api.model.kubernetes;

import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;

public abstract class KubeApiResource<A extends KubeAttributes, R> extends JsonApiResource<A, R> {

    protected KubeApiResource(String id, String type, JsonApiMeta meta, A attributes, R relationships) {
        super(id, type, meta, attributes, relationships);
    }

    protected KubeApiResource(String id, String type, A attributes, R relationships) {
        super(id, type, attributes, relationships);
    }

    protected KubeApiResource(String id, String type, JsonApiMeta meta, A attributes) {
        super(id, type, meta, attributes);
    }

    protected KubeApiResource(String id, String type, A attributes) {
        super(id, type, attributes);
    }

    public void setManaged(Boolean managed) {
        addMeta("managed", managed);
    }

    public String name() {
        return getAttributes().getName();
    }

    public void name(String name) {
        getAttributes().setName(name);
    }

    public String namespace() {
        return getAttributes().getNamespace();
    }

    public void namespace(String namespace) {
        getAttributes().setNamespace(namespace);
    }

    public String creationTimestamp() {
        return getAttributes().getCreationTimestamp();
    }

    public void creationTimestamp(String creationTimestamp) {
        getAttributes().setCreationTimestamp(creationTimestamp);
    }
}
