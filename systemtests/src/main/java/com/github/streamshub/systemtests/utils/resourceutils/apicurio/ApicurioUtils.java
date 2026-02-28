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

    /**
     * Creates a new artifact in the specified registry group with an initial version.
     *
     * <p>This method constructs a {@code CreateArtifact} request, sets its artifact ID and type,
     * attaches the provided schema as the first version with the given content type,
     * and submits it using the provided {@link RegistryClient}.</p>
     *
     * @param client       the registry client used to communicate with the artifact registry
     * @param groupId      the ID of the registry group where the artifact will be created
     * @param artifactId   the unique identifier of the artifact
     * @param artifactType the type of the artifact (e.g. AVRO, JSON, PROTOBUF)
     * @param schema       the schema or artifact content
     * @param contentType  the content type of the artifact (e.g. application/json)
     */
    public static void createArtifact(RegistryClient client, String groupId, String artifactId, String artifactType, String schema, String contentType) {
        LOGGER.info("Creating artifact {}  of type {} in Apicurio Registry", artifactId, artifactType);
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

    /**
     * Builds a Kafka producer configuration string for use with Apicurio Registry.
     *
     * <p>The configuration includes registry connection details, artifact settings,
     * and optional serializer and SCRAM-SHA authentication configuration.</p>
     *
     * <p>If {@code scramShaConfig} or {@code serializer} are {@code null},
     * they are omitted from the resulting configuration.</p>
     *
     * @param registryUrl   the URL of the Apicurio Registry instance
     * @param serializer    the fully qualified class name of the value serializer (nullable)
     * @param artifactId    the artifact ID to use when interacting with the registry
     * @param scramShaConfig optional SCRAM-SHA authentication configuration block (nullable)
     * @return a formatted Kafka producer configuration string
     */
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

    /**
     * Retrieves the content ID of a specific artifact version from the registry.
     *
     * <p>Queries the registry for the given group ID, artifact ID, and version,
     * and returns the associated content ID as a string.</p>
     *
     * @param client     the registry client used to communicate with the artifact registry
     * @param groupId    the ID of the registry group
     * @param artifactId the ID of the artifact
     * @param version    the artifact version (or version expression)
     * @return the content ID of the specified artifact version as a string
     * @throws InvalidParameterException if the content ID is not found
     */
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
