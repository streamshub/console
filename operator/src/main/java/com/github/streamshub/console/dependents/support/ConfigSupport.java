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
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.reconciler.Context;

public class ConfigSupport {

    private ConfigSupport() {
    }

    public static String encodeString(String value) {
        return encodeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeBytes(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    public static String decodeString(String encodedValue) {
        return new String(decodeBytes(encodedValue), StandardCharsets.UTF_8);
    }

    public static byte[] decodeBytes(String encodedValue) {
        return Base64.getDecoder().decode(encodedValue);
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
     * Fetch the value from the given valueSpec. The return value
     * will be the decoded raw bytes from the data source.
     */
    public static byte[] getValue(Context<Console> context, String namespace, Value valueSpec) {
        if (valueSpec == null) {
            return null; // NOSONAR : empty array is not wanted when the valueSpec is null
        }

        return Optional.ofNullable(valueSpec.getValue())
                .map(ConfigSupport::toBytes)
            .or(() -> Optional.ofNullable(valueSpec.getValueFrom())
                    .map(ValueReference::getConfigMapKeyRef)
                    .flatMap(ref -> getValue(context, namespace, ref)))
            .or(() -> Optional.ofNullable(valueSpec.getValueFrom())
                    .map(ValueReference::getSecretKeyRef)
                    .flatMap(ref -> getValue(context, namespace, ref)))
            .orElse(null);
    }

    private static byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Optional<byte[]> getValue(Context<Console> context,
            String namespace,
            ConfigMapKeySelector ref) {

        ConfigMap source = getResource(context, ConfigMap.class, namespace, ref.getName(), Boolean.TRUE.equals(ref.getOptional()));

        if (source != null) {
            return Optional.ofNullable(source.getData())
                    .map(data -> data.get(ref.getKey()))
                    .map(ConfigSupport::toBytes)
                .or(() -> Optional.ofNullable(source.getBinaryData())
                    .map(data -> data.get(ref.getKey()))
                    .map(ConfigSupport::decodeBytes));
        }

        return Optional.empty();
    }

    private static Optional<byte[]> getValue(Context<Console> context,
            String namespace,
            SecretKeySelector ref) {

        Secret source = getResource(context, Secret.class, namespace, ref.getName(), Boolean.TRUE.equals(ref.getOptional()));

        if (source != null) {
            return Optional.ofNullable(source.getData())
                    .map(data -> data.get(ref.getKey()))
                    .map(ConfigSupport::decodeBytes);
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
