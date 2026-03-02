package com.github.streamshub.systemtests.setup.kroxylicious;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kroxy.KroxyNamingUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.kroxylicious.kubernetes.api.common.FilterRefBuilder;
import io.kroxylicious.kubernetes.api.common.KafkaServiceRefBuilder;
import io.kroxylicious.kubernetes.api.common.Protocol;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProtocolFilterBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngressBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaServiceBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class KroxyResourcesSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KroxyResourcesSetup.class);
    private KroxyResourcesSetup() {}

    public static void setupDefaultProxyResourcesWithAuthIfNeeded(String namespace, String kafkaName, String kafkaUser,
        String kafkaProxyName, String kafkaProxyIngressName, String kafkaServiceName, String virtualClusterName, String kafkaProtocolFilterName) {


        if (ResourceUtils.getKubeResource(KafkaProxy.class, namespace, kafkaProxyName) != null) {
            LOGGER.warn("Skipping kroxy resources deployment, already present");
            return;
        }
        LOGGER.info("Setup test kroxy resource components");

        // Must be deployed in this order for kroxy to work
        String aclConfigMapName = KroxyNamingUtils.aclConfigMapName(namespace);
        KubeResourceManager.get().createResourceWithWait(getAclConfigMap(namespace, aclConfigMapName, kafkaUser).build());
        KubeResourceManager.get().createResourceWithWait(getSaslInspectorFilter(namespace, kafkaProtocolFilterName).build());
        KubeResourceManager.get().createResourceWithWait(getAuthorizationFilter(namespace, aclConfigMapName, kafkaProtocolFilterName).build());

        KubeResourceManager.get().createResourceWithWait(getDefaultKafkaProxy(namespace, kafkaProxyName, Constants.REGULAR_KAFKA_PROXY_REPLICAS).build());
        KubeResourceManager.get().createResourceWithWait(getDefaultKafkaProxyIngress(namespace, kafkaProxyIngressName, kafkaProxyName).build());
        KubeResourceManager.get().createResourceWithWait(getDefaultKafkaService(namespace, kafkaServiceName, kafkaName).build());
        KubeResourceManager.get().createResourceWithWait(getDefaultVirtualKafkaCluster(namespace, virtualClusterName, kafkaServiceName, kafkaProxyName, kafkaProxyIngressName, kafkaProtocolFilterName).build());

        // Wait for KafkaProxy pod to be stable
        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getkafkaProxyPodLabelSelector(kafkaProxyName), Constants.REGULAR_KAFKA_PROXY_REPLICAS, true);
    }

    public static VirtualKafkaClusterBuilder getDefaultVirtualKafkaCluster(
        String namespace, String virtualKafkaClusterName, String kafkaServiceName, String kafkaProxyName, String ingressName, String kafkaProtocolFilterName) {
        return new VirtualKafkaClusterBuilder()
            .withNewMetadata()
                .withName(virtualKafkaClusterName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withTargetKafkaServiceRef(new KafkaServiceRefBuilder()
                        .withName(kafkaServiceName)
                        .build())
                .withNewProxyRef()
                    .withName(kafkaProxyName)
                .endProxyRef()
            .addNewIngress()
                .withNewIngressRef()
                    .withName(ingressName)
                .endIngressRef()
            .endIngress()
            .addToFilterRefs(new FilterRefBuilder().withName(kafkaProtocolFilterName + "-auth").build())
            .addToFilterRefs(new FilterRefBuilder().withName(kafkaProtocolFilterName + "-inspect").build())
            .endSpec();
    }

    public static KafkaProxyBuilder getDefaultKafkaProxy(String namespace, String kafkaProxyName, int replicas) {
        return new KafkaProxyBuilder()
            .withNewMetadata()
                .withName(kafkaProxyName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withReplicas(replicas)
            .endSpec();
    }

    public static KafkaProxyIngressBuilder getDefaultKafkaProxyIngress(String namespace, String kafkaProxyIngressName, String kafkaProxyName) {
        return new KafkaProxyIngressBuilder()
            .withNewMetadata()
                .withName(kafkaProxyIngressName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withNewClusterIP()
                    .withProtocol(Protocol.TCP)
                .endClusterIP()
                .withNewProxyRef()
                    .withName(kafkaProxyName)
                .endProxyRef()
            .endSpec();
    }

    public static KafkaServiceBuilder getDefaultKafkaService(String namespace, String kafkaServiceName, String kafkaName) {
        return new KafkaServiceBuilder()
            .withNewMetadata()
                .withName(kafkaServiceName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withBootstrapServers(KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName))
            .endSpec();
    }

    public static KafkaProtocolFilterBuilder getDefaultKafkaProtocolFilter(String namespace, String kafkaProtocolFilterName) {
        return new KafkaProtocolFilterBuilder()
            .withNewMetadata()
                .withName(kafkaProtocolFilterName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withType("io.kroxylicious.filter.simpletransform.ProduceRequestTransformation")
                .withConfigTemplate(Map.of("findPattern", "foo", "replacementValue", "bar"))
            .endSpec();
    }

    public static KafkaProtocolFilterBuilder getSaslInspectorFilter(String namespace, String kafkaProtocolFilterName) {
        return getDefaultKafkaProtocolFilter(namespace, kafkaProtocolFilterName + "-inspect")
            .withNewSpec()
                .withType("SaslInspection")
                .withConfigTemplate(Map.of("enabledMechanisms", Set.of("SCRAM-SHA-512"), "requireAuthentication", true))
            .endSpec();
    }

    public static KafkaProtocolFilterBuilder getAuthorizationFilter(String namespace, String aclConfigMapName, String kafkaProtocolFilterName) {
        return getDefaultKafkaProtocolFilter(namespace, kafkaProtocolFilterName + "-auth")
            .withNewSpec()
                .withType("Authorization")
                .withConfigTemplate(Map.of("authorizer", "AclAuthorizerService",
                "authorizerConfig", Map.of("aclFile",
                "${" + ConfigMap.class.getSimpleName().toLowerCase(Locale.ROOT) + ":" + aclConfigMapName + ":" + Constants.ACL_CONGIF_MAP_RULES_FIELD + "}"
                )))
            .endSpec();
    }

    public static ConfigMapBuilder getAclConfigMap(String namespace, String aclConfigMapName, String userName) {
        String aclRules =
            "from io.kroxylicious.filter.authorization import TopicResource as Topic;\n" +
            "allow User with name = \"" + userName + "\" to * Topic with name like \"*\";\n" +
            "otherwise deny;";

        return new ConfigMapBuilder()
            .withNewMetadata()
                .withName(aclConfigMapName)
                .withNamespace(namespace)
            .endMetadata()
            .addToData(Map.of(Constants.ACL_CONGIF_MAP_RULES_FIELD, aclRules));
    }
}
