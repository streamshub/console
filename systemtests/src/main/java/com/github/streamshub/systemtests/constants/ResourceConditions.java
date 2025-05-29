package com.github.streamshub.systemtests.constants;

import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.enums.ResourceStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.skodjob.testframe.resources.ResourceCondition;
import io.strimzi.api.kafka.model.common.Spec;
import io.strimzi.api.kafka.model.kafka.Status;

public class ResourceConditions {
    private ResourceConditions() {}

    /**
     * Creates a {@link ResourceCondition} that checks whether a custom resource has a specific status condition.
     *
     * @param customResourceStatus the expected {@link ResourceStatus} (e.g., READY, WARNING)
     * @param conditionStatus      the expected {@link ConditionStatus} (e.g., TRUE, FALSE, UNKNOWN)
     * @param <T>                  the type of the {@link CustomResource}, extending {@code CustomResource<Spec, Status>}
     * @return a {@link ResourceCondition} that evaluates to {@code true} if the resource contains a matching condition
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> ResourceCondition<T> resourceHasDesiredState(ResourceStatus customResourceStatus, ConditionStatus conditionStatus) {
        return new ResourceCondition<>(resource ->
            checkMatchingConditions(resource, customResourceStatus, conditionStatus),
            String.format("%s state with condition status: %s", customResourceStatus.toString(), conditionStatus.toString())
        );
    }

    /**
     * Checks whether the given custom resource has a condition matching the specified type and status.
     *
     * @param resource             the custom resource to evaluate
     * @param customResourceStatus the condition type to match (e.g., {@code ResourceStatus.READY})
     * @param conditionStatus      the condition status to match (e.g., {@code ConditionStatus.TRUE})
     * @param <T>                  the type of the {@link CustomResource}, extending {@code CustomResource<Spec, Status>}
     * @return {@code true} if the resource contains a matching condition; {@code false} otherwise
     */
    public static <T extends CustomResource<? extends Spec, ? extends Status>> boolean checkMatchingConditions(T resource, ResourceStatus customResourceStatus, ConditionStatus conditionStatus) {
        return resource.getStatus()
            .getConditions()
            .stream()
            .anyMatch(condition ->
                condition.getType().equals(customResourceStatus.toString()) &&
                condition.getStatus().equals(conditionStatus.toString()));
    }
}
