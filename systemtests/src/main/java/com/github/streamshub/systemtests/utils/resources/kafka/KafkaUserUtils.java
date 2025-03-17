package com.github.streamshub.systemtests.utils.resources.kafka;

import com.github.streamshub.systemtests.constants.Constants;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserList;

public class KafkaUserUtils {
    // -------------
    // Client
    // -------------
    public static MixedOperation<KafkaUser, KafkaUserList, Resource<KafkaUser>> kafkaUserClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(KafkaUser.class, KafkaUserList.class);
    }

    // -------------
    // Get
    // -------------
    public static KafkaUser getUser(String namespace, String name) {
        return kafkaUserClient().inNamespace(namespace).withName(name).get();
    }
    public static String kafkaUserName(String kafkaName) {
        return kafkaName + "-" + Constants.USER;
    }
}
