package com.github.eyefloaters.console.api.support;

import java.net.URI;

import com.github.eyefloaters.console.api.model.ListFetchParams;

public class ListRequestContext {

    final URI requestUri;
    final String sort;

    public ListRequestContext(URI requestUri, ListFetchParams listParams) {
        this.requestUri = requestUri;
        sort = listParams.getSort();
    }

    public String getSort() {
        return sort;
    }
}
