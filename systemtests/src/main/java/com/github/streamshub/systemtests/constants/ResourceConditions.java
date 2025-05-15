package com.github.streamshub.systemtests.constants;

import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.enums.ResourceStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.skodjob.testframe.resources.ResourceCondition;
import io.strimzi.api.kafka.model.common.Spec;
import io.strimzi.api.kafka.model.kafka.Status;

public class ResourceConditions {
    private ResourceConditions() {}

    public static <T extends CustomResource<? extends Spec, ? extends Status>> ResourceCondition<T> resourceHasDesiredState(ResourceStatus customResourceStatus, ConditionStatus conditionStatus) {
        return new ResourceCondition<>(resource ->
            checkMatchingConditions(resource, customResourceStatus, conditionStatus),
            String.format("%s state with condition status: %s", customResourceStatus.toString(), conditionStatus.toString())
        );
    }

    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean checkMatchingConditions(T resource, ResourceStatus customResourceStatus, ConditionStatus conditionStatus) {
        return resource.getStatus()
            .getConditions()
            .stream()
            .anyMatch(condition -> condition.getType().equals(customResourceStatus.toString()) && condition.getStatus().equals(conditionStatus.toString()));
    }
}
