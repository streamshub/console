package com.github.streamshub.systemtests.utils.resources;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.resources.ResourceOperation;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.strimzi.api.kafka.model.common.Spec;
import io.strimzi.api.kafka.model.kafka.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceManagerUtils {

    private static final Logger LOGGER = LogManager.getLogger(ResourceManagerUtils.class);

    // --------------------------------------
    // ---------------- Wait ----------------
    // --------------------------------------

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, T resource,
                                                                                                                  long resourceTimeout) {
        return waitForResourceStatusReady(operation, resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName(),
            ConditionStatus.True, resourceTimeout);
    }

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, String kind,
                                                                                                                  String namespace, String name,
                                                                                                                  ConditionStatus conditionStatus,
                                                                                                                  long resourceTimeoutMs) {
        LOGGER.info("Waiting for {}: {}/{} will have desired state 'Ready'", kind, namespace, name);

        CommonUtils.waitFor(String.format("%s: %s/%s will have desired state 'Ready'", kind, namespace, name),
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, resourceTimeoutMs,
            () -> {
                final Status status = operation.inNamespace(namespace)
                    .withName(name)
                    .get()
                    .getStatus();
                if (status != null) {
                    return status.getConditions().stream().anyMatch(condition -> condition.getType().equals("Ready") && condition.getStatus().equals(conditionStatus.toString()));
                }
                return false;
            });

        LOGGER.info("{}: {}/{} is in desired state 'Ready'", kind, namespace, name);
        return true;
    }
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatusReady(MixedOperation<T, ?, ?> operation, T resource) {
        long resourceTimeout = ResourceOperation.getTimeoutForResourceReadiness(resource.getKind());
        return waitForResourceStatusReady(operation, resource, resourceTimeout);
    }

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatus(MixedOperation<T, ?, ?> operation, T resource, Enum<?> status) {
        long resourceTimeout = ResourceOperation.getTimeoutForResourceReadiness(resource.getKind());
        return waitForResourceStatus(operation, resource, status, resourceTimeout);
    }

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatus(MixedOperation<T, ?, ?> operation, T resource, Enum<?> statusType, long resourceTimeout) {
        return waitForResourceStatus(operation, resource.getKind(), resource.getMetadata().getNamespace(), resource.getMetadata().getName(), statusType, ConditionStatus.True, resourceTimeout);
    }

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean waitForResourceStatus(MixedOperation<T, ?, ?> operation, String kind, String namespace, String name, Enum<?> statusType, ConditionStatus conditionStatus, long resourceTimeoutMs) {
        LOGGER.info("Waiting for {}: {}/{} will have desired state: {}", kind, namespace, name, statusType);

        CommonUtils.waitFor(String.format("%s: %s#%s will have desired state: %s", kind, namespace, name, statusType),
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, resourceTimeoutMs,
            () -> {
                T res = operation.inNamespace(namespace).withName(name).get();
                if (res != null && res.getStatus() != null && res.getStatus().getConditions() != null && !res.getStatus().getConditions().isEmpty()) {
                    return operation.inNamespace(namespace).withName(name).get().getStatus().getConditions()
                    .stream().anyMatch(condition ->
                        condition.getType().equals(statusType.toString())
                        && condition.getStatus().equals(conditionStatus.toString())
                        );
                }
                return false;
            },
            () -> {
            });

        LOGGER.info("{}: {}/{} is in desired state: {}", kind, namespace, name, statusType);
        return true;
    }
}
