package com.github.streamshub.console.api.model.jsonapi;

import java.util.List;

public class JsonApiRootDataList<T> extends JsonApiRootData<List<T>> {

    protected JsonApiRootDataList(List<T> data) {
        super(data);
    }
}
