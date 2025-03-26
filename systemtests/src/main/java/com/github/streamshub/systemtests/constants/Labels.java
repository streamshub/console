package com.github.streamshub.systemtests.constants;

import io.strimzi.api.ResourceLabels;

public class Labels {
    private Labels() {}
    public static final String COLLECT_ST_LOGS = "streamshub-st";

    // ------------
    // Strimzi
    // ------------
    public static final String STRIMZI_POOL_NAME_LABEL = ResourceLabels.STRIMZI_DOMAIN + "pool-name";
    public static final String STRIMZI_BROKER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "broker-role";
    public static final String STRIMZI_CONTROLLER_ROLE_LABEL = ResourceLabels.STRIMZI_DOMAIN + "controller-role";
}
