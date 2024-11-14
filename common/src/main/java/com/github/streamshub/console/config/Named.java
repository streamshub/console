package com.github.streamshub.console.config;

import java.util.Collection;

public interface Named {

    static boolean uniqueNames(Collection<? extends Named> items) {
        if (items == null) {
            return true;
        }
        return items.stream().map(Named::getName).distinct().count() == items.size();
    }

    String getName();

}
