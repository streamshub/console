package com.github.streamshub.systemtests.utils.resourceutils.apicurio;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.CreateArtifact;
import io.apicurio.registry.rest.client.models.CreateVersion;
import io.apicurio.registry.rest.client.models.VersionContent;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.apache.logging.log4j.Logger;

import java.security.InvalidParameterException;

public class ApicurioUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioUtils.class);

    private static final int APICURIO_PORT = 8080;
    private static final String APICURIO_APP_SERVICE_SUFFIX = "-app-service";
    private static final String APICURIO_APP_INGRESS_SUFFIX = "-app-ingress";

    private ApicurioUtils() { }

    /**
     * Returns the in-cluster service hostname for the given Apicurio Registry instance.
     * e.g. "apicurio-registry-service.my-namespace.svc:8080"
     *
     * @param namespace    namespace where the ApicurioRegistry CR was deployed
     * @param registryName name of the ApicurioRegistry CR
     * @return in-cluster service host:port string
     */
    public static String getApicurioServiceUrl(String namespace, String registryName) {
        String serviceName = registryName + APICURIO_APP_SERVICE_SUFFIX;
        Service service = ResourceUtils.getKubeResource(Service.class, namespace, serviceName);
        if (service == null) {
            throw new IllegalStateException(
                "Could not find Apicurio Registry app service for '" + registryName + "' in namespace '" + namespace + "'"
            );
        }
        String host = service.getMetadata().getName() + "." + namespace + ".svc";
        LOGGER.info("Resolved Apicurio Registry service URL: {}:{}", host, APICURIO_PORT);
        return host + ":" + APICURIO_PORT;
    }

    /**
     * Returns the full Apicurio Registry v3 API URL for use in Console schema registry config.
     * e.g. "http://apicurio-registry-/apis/registry/v3"
     *
     * @param namespace    namespace where the ApicurioRegistry CR was deployed
     * @param registryName name of the ApicurioRegistry CR
     * @return full registry API URL
     */
    public static String getApicurioRegistryUrl(String namespace, String registryName) {
        return "http://" + ResourceUtils.getKubeResource(Ingress.class, namespace, registryName + APICURIO_APP_INGRESS_SUFFIX)
            .getSpec()
            .getRules()
            .getFirst()
            .getHost();
    }

    public static String getApicurioRegistryUrlWithApi(String namespace, String registryName) {
        return getApicurioRegistryUrl(namespace, registryName) + Constants.APICURIO_API_V3_SUFFIX;
    }

    public static void createArtifact(RegistryClient client, String groupId, String artifactId, String artifactType, String schema, String contentType) {
        CreateArtifact createArtifact = new CreateArtifact();
        createArtifact.setArtifactId(artifactId);
        createArtifact.setArtifactType(artifactType);

        VersionContent content = new VersionContent();
        content.setContent(schema);
        content.setContentType(contentType);

        CreateVersion createVersion = new CreateVersion();
        createVersion.setContent(content);
        createArtifact.setFirstVersion(createVersion);

        client.groups().byGroupId(groupId).artifacts().post(createArtifact);
    }

    public static String getApicurioProducerConfig(String registryUrl, String serializer, String artifactId, String scramShaConfig) {
        return """
            %s
            %s
            apicurio.registry.url=%s
            apicurio.registry.group-id=default
            apicurio.registry.artifact-id=%s
            apicurio.registry.artifact-version=1
            apicurio.registry.headers.enabled=true
            apicurio.registry.find-latest=true
            apicurio.registry.auto-register=false
            """
            .formatted(
                scramShaConfig != null ? scramShaConfig : "",
                serializer != null ? "value.serializer=" + serializer : "",
                registryUrl,
                artifactId
            );
    }

    public static String getArtifactContentId(RegistryClient client, String groupId, String artifactId, String version) {
        Long contentId = client.groups()
            .byGroupId(groupId)
            .artifacts()
            .byArtifactId(artifactId)
            .versions()
            .byVersionExpression(version)
            .get()
            .getContentId();

        if (contentId == null) {
            throw new InvalidParameterException("ContentId not found for artifact %s/%s version %s".formatted(groupId, artifactId, version));
        }

        return contentId.toString();
    }
}
