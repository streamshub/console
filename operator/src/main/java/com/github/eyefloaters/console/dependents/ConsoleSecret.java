package com.github.eyefloaters.console.dependents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleSecret extends CRUDKubernetesDependentResource<Secret, Console> implements ConsoleResource {

    private static final Random RANDOM = new Random();

    public ConsoleSecret() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(Console primary, Context<Console> context) {
        var nextAuth = context.getSecondaryResource(Secret.class).map(s -> s.getData().get("NEXTAUTH_SECRET"));
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(name(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("NEXTAUTH_SECRET", nextAuth.orElseGet(() -> Base64.getEncoder().encodeToString(base64String(32).getBytes(StandardCharsets.UTF_8))))
                .build();
    }

    private static String base64String(int length) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        OutputStream out = Base64.getEncoder().wrap(buffer);

        RANDOM.ints().limit(length).forEach(value -> {
            try {
                out.write(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return new String(buffer.toByteArray()).substring(0, length);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-secret";
    }
}