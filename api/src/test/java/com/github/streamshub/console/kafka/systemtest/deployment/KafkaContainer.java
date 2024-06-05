package com.github.streamshub.console.kafka.systemtest.deployment;

import io.strimzi.test.container.StrimziKafkaContainer;

class KafkaContainer extends StrimziKafkaContainer {

    KafkaContainer(String imageReference) {
        super(imageReference);
    }

    String getCACertificate() {
        return null;
    }

}
