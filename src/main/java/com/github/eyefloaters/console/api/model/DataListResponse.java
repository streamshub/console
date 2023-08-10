package com.github.eyefloaters.console.api.model;

import java.util.List;

abstract class DataListResponse<T> {

    private final List<T> data;

    protected DataListResponse(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return data;
    }
}
