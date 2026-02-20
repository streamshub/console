package com.github.streamshub.systemtests.annotations;

import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OpenShiftOnlyCondition implements ExecutionCondition {
    private static final Logger LOGGER = LogManager.getLogger(OpenShiftOnlyCondition.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

        if (ClusterUtils.isOcp()) {
            return ConditionEvaluationResult.enabled("Test is enabled");
        } else {
            LOGGER.info("{} is @OpenShiftOnly, but the running cluster is not OpenShift: Ignoring {}",
                    extensionContext.getDisplayName(),
                    extensionContext.getDisplayName()
            );
            return ConditionEvaluationResult.disabled("Test is disabled");
        }
    }
}
