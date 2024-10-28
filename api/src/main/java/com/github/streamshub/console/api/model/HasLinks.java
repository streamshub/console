package com.github.streamshub.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

interface HasLinks<T> {

    Map<String, String> links();

    void links(Map<String, String> links);

    default T addLink(String key, String value) {
        links(addEntry(links(), key, value));
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }

    private static <K, V> Map<K, V> addEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put(key, value);
        return map;
    }
}
