package com.github.streamshub.console.config;

import java.util.Collection;

public interface Named {

    static boolean uniqueNames(Collection<? extends Named> configs) {
        if (configs == null) {
            return true;
        }
        return configs.stream().map(Named::getName).distinct().count() == configs.size();
    }

    String getName();

}
