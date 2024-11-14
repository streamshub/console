package com.github.streamshub.console.dependents;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

abstract class BaseLabelDiscriminator<R extends HasMetadata> implements ResourceDiscriminator<R, HasMetadata> {

    private final Map<String, String> matchLabels;

    protected BaseLabelDiscriminator(String label, String matchValue) {
        this.matchLabels = Map.of(label, matchValue);
    }

    protected BaseLabelDiscriminator(Map<String, String> matchLabels) {
        this.matchLabels = Map.copyOf(matchLabels);
    }

    public Optional<R> distinguish(Class<R> resourceType,
            HasMetadata primary,
            Context<HasMetadata> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(this::matches)
                .findFirst();
    }

    private boolean matches(HasMetadata resource) {
        return matchLabels.entrySet()
                .stream()
                .allMatch(label -> label.getValue().equals(resource.getMetadata().getLabels().get(label.getKey())));
    }
}
