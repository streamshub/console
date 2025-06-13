package com.github.streamshub.console.api.model.jsonapi;

import java.util.ArrayList;
import java.util.List;

public class JsonApiRootDataList<T> extends JsonApiRootData<List<T>> {

    public JsonApiRootDataList() {
        super(new ArrayList<>());
    }

    protected JsonApiRootDataList(List<T> data) {
        super(data);
    }
}
