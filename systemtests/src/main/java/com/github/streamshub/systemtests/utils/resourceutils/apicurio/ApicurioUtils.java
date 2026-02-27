package com.github.streamshub.systemtests.utils.resourceutils.apicurio;

import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Service;
import org.apache.logging.log4j.Logger;

public class ApicurioUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioUtils.class);

    private static final int APICURIO_PORT = 8080;
    private static final String APICURIO_API_PATH = "/apis/registry/v3";
    private static final String APICURIO_APP_SERVICE_SUFFIX = "-app-service";

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
     * Returns the full Apicurio Registry v2 API URL for use in Console schema registry config.
     * e.g. "http://apicurio-registry-service.my-namespace.svc:8080/apis/registry/v2"
     *
     * @param namespace    namespace where the ApicurioRegistry CR was deployed
     * @param registryName name of the ApicurioRegistry CR
     * @return full registry API URL
     */
    public static String getApicurioRegistryApiUrl(String namespace, String registryName) {
        return "http://" + getApicurioServiceUrl(namespace, registryName) + APICURIO_API_PATH;
    }
}
