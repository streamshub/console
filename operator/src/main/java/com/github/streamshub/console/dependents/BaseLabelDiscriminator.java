package com.github.streamshub.console.dependents;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

abstract class BaseLabelDiscriminator<R extends HasMetadata> implements ResourceDiscriminator<R, HasMetadata> {

    private final String label;
    private final String matchValue;

    protected BaseLabelDiscriminator(String label, String matchValue) {
        this.label = label;
        this.matchValue = matchValue;
    }

    public Optional<R> distinguish(Class<R> resourceType,
            HasMetadata primary,
            Context<HasMetadata> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> matchValue.equals(d.getMetadata().getLabels().get(label)))
                .findFirst();
    }
}
