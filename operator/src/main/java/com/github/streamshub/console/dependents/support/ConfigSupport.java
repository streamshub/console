package com.github.streamshub.console.dependents.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.ConfigVars;
import com.github.streamshub.console.api.v1alpha1.spec.Value;
import com.github.streamshub.console.api.v1alpha1.spec.ValueReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
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

        source.getValuesFrom().forEach(fromSource -> {
            String prefix = fromSource.getPrefix();
            var configMapRef = fromSource.getConfigMapRef();
            var secretRef = fromSource.getSecretRef();

            if (configMapRef != null) {
                copyData(target, getData(context, namespace, configMapRef), prefix, false);
            }

            if (secretRef != null) {
                copyData(target, getData(context, namespace, secretRef), prefix, true);
            }
        });

        source.getValues().forEach(configVar -> target.put(configVar.getName(), configVar.getValue()));
    }

    private static void copyData(Map<String, String> target, Map<String, String> source, String prefix, boolean decode) {
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

    private static Map<String, String> getData(Context<Console> context,
            String namespace,
            ConfigMapEnvSource ref) {

        ConfigMap source = getResource(context, ConfigMap.class, namespace, ref.getName(), Boolean.TRUE.equals(ref.getOptional()));

        return Optional.ofNullable(source)
                .map(ConfigMap::getData)
                .orElseGet(Collections::emptyMap);
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

    private static Map<String, String> getData(Context<Console> context,
            String namespace,
            SecretEnvSource ref) {

        Secret source = getResource(context, Secret.class, namespace, ref.getName(), Boolean.TRUE.equals(ref.getOptional()));

        return Optional.ofNullable(source)
                .map(Secret::getData)
                .orElseGet(Collections::emptyMap);
    }

    public static <T extends HasMetadata> boolean resourceSupported(Context<Console> context, Class<T> resourceType) {
        return context.getClient().supports(resourceType);
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

    public static boolean dexIntegrationEnabled(Console primary) {
        var security = primary.getSpec().getSecurity();

        if (security == null) {
            return false;
        }

        return security.getOpenshift() != null || security.getLdap() != null;
    }

    public static boolean dexOpenshiftIntegrationEnabled(Console primary) {
        var security = primary.getSpec().getSecurity();

        if (security == null) {
            return false;
        }

        return security.getOpenshift() != null;
    }

    /**
     * Check whether user-provided pass-through TLS is in effect for the Console CR.
     * This is the case when the TLS certificate has been provided in the CR and Dex
     * is not enabled (i.e. there is no need to have two routes with pass-based
     * routing and re-encrypt TLS).
     */
    public static boolean userProvidedPassthroughTLS(Console primary) {
        return !dexIntegrationEnabled(primary) && primary.getSpec().getTls() != null;
    }

    /**
     * Check whether an OpenShift Route with re-encrypt TLS is in effect for the Console
     * CR. This will be used when the operator is running in OpenShift and either Dex is
     * enabled (we need to re-encrypt for path-based routing) or not user-provided TLS 
     * certificate is available (we re-encrypt the backend where the user-facing ingress
     * is using the OpenShift provided certificate).
     */
    public static boolean servingCertificateReencryptTLS(Console primary, Context<Console> context) {
        return !userProvidedPassthroughTLS(primary) && resourceSupported(context, Route.class);
    }
}
