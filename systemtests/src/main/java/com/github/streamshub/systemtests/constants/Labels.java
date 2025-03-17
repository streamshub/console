package com.github.streamshub.systemtests.constants;

import static io.strimzi.api.ResourceLabels.STRIMZI_DOMAIN;

public interface Labels {
    // ------------
    // Strimzi
    // ------------
    String STRIMZI_CLUSTER = STRIMZI_DOMAIN + "cluster";
    String STRIMZI_POOL_NAME = STRIMZI_DOMAIN + "pool-name";
    String STRIMZI_KIND = STRIMZI_DOMAIN + "kind";
    String STRIMZI_COMPONENT_TYPE = STRIMZI_DOMAIN + "component-type";
    String STRIMZI_BROKER_ROLE = STRIMZI_DOMAIN + "broker-role";
    String STRIMZI_CONTROLLER_ROLE = STRIMZI_DOMAIN + "controller-role";
}
