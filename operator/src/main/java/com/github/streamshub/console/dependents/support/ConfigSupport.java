package com.github.streamshub.console.dependents.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.ConfigVars;
import com.github.streamshub.console.api.v1alpha1.spec.Value;
import com.github.streamshub.console.api.v1alpha1.spec.ValueReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.reconciler.Context;

public class ConfigSupport {

    private ConfigSupport() {
    }

    public static String encodeString(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeString(String encodedValue) {
        return new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
    }

    public static void setConfigVars(Console primary, Context<Console> context, Map<String, String> target, ConfigVars source) {
        String namespace = primary.getMetadata().getNamespace();

        source.getValuesFrom().stream().forEach(fromSource -> {
            String prefix = fromSource.getPrefix();
            var configMapRef = fromSource.getConfigMapRef();
            var secretRef = fromSource.getSecretRef();

            if (configMapRef != null) {
                copyData(context, target, ConfigMap.class, namespace, configMapRef.getName(), prefix, configMapRef.getOptional(), ConfigMap::getData);
            }

            if (secretRef != null) {
                copyData(context, target, Secret.class, namespace, secretRef.getName(), prefix, secretRef.getOptional(), Secret::getData);
            }
        });

        source.getValues().forEach(configVar -> target.put(configVar.getName(), configVar.getValue()));
    }

    @SuppressWarnings("java:S107") // Ignore Sonar warning for too many args
    public static <S extends HasMetadata> void copyData(Context<Console> context,
            Map<String, String> target,
            Class<S> sourceType,
            String namespace,
            String name,
            String prefix,
            Boolean optional,
            Function<S, Map<String, String>> dataProvider) {

        S source = getResource(context, sourceType, namespace, name, Boolean.TRUE.equals(optional));

        if (source != null) {
            copyData(target, dataProvider.apply(source), prefix, Secret.class.equals(sourceType));
        }
    }

    public static void copyData(Map<String, String> target, Map<String, String> source, String prefix, boolean decode) {
        source.forEach((key, value) -> {
            if (prefix != null) {
                key = prefix + key;
            }
            target.put(key, decode ? decodeString(value) : value);
        });
    }

    /**
     * Fetch the string value from the given valueSpec. The return string
     * will be encoded for use in the Console secret data map.
     */
    public static String getValue(Context<Console> context, String namespace, Value valueSpec) {
        if (valueSpec == null) {
            return null;
        }

        return Optional.ofNullable(valueSpec.getValue())
                .map(ConfigSupport::encodeString)
            .or(() -> Optional.ofNullable(valueSpec.getValueFrom())
                    .map(ValueReference::getConfigMapKeyRef)
                    .flatMap(ref -> getValue(context, ConfigMap.class, namespace, ref.getName(), ref.getKey(), ref.getOptional(), ConfigMap::getData)
                            .map(ConfigSupport::encodeString)
                        .or(() -> getValue(context, ConfigMap.class, namespace, ref.getName(), ref.getKey(), ref.getOptional(), ConfigMap::getBinaryData))))
            .or(() -> Optional.ofNullable(valueSpec.getValueFrom())
                    .map(ValueReference::getSecretKeyRef)
                    .flatMap(ref -> getValue(context, Secret.class, namespace, ref.getName(), ref.getKey(), ref.getOptional(), Secret::getData))
                    /* No need to call encodeString, the value is already encoded from Secret */)
            .orElse(null);
    }

    public static <S extends HasMetadata> Optional<String> getValue(Context<Console> context,
            Class<S> sourceType,
            String namespace,
            String name,
            String key,
            Boolean optional,
            Function<S, Map<String, String>> dataProvider) {

        S source = getResource(context, sourceType, namespace, name, Boolean.TRUE.equals(optional));

        if (source != null) {
            return Optional.ofNullable(dataProvider.apply(source).get(key));
        }

        return Optional.empty();
    }

    public static <T extends HasMetadata> T getResource(
            Context<Console> context, Class<T> resourceType, String namespace, String name) {
        return getResource(context, resourceType, namespace, name, false);
    }

    public static <T extends HasMetadata> T getResource(
            Context<Console> context, Class<T> resourceType, String namespace, String name, boolean optional) {

        T resource;

        try {
            resource = context.getClient()
                .resources(resourceType)
                .inNamespace(namespace)
                .withName(name)
                .get();
        } catch (KubernetesClientException e) {
            throw new ReconciliationException("Failed to retrieve %s resource: %s/%s. Message: %s"
                    .formatted(resourceType.getSimpleName(), namespace, name, e.getMessage()));
        }

        if (resource == null && !optional) {
            throw new ReconciliationException("No such %s resource: %s/%s".formatted(resourceType.getSimpleName(), namespace, name));
        }

        return resource;
    }

}
