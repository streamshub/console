package com.github.streamshub.systemtests.setup;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private final String resourceURL = "https://github.com/strimzi/strimzi-kafka-operator/tree/" + Environment.STRIMZI_OPERATOR_VERSION + "/packaging/install/cluster-operator";
    private final List<String> strimziResources = getStrimziYamlFilesContent();
    private final String deploymentNamespace;
    private final String deploymentName;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;
    }

    public void setup() {
        LOGGER.info("----------- Setup Strimzi Cluster Operator -----------");
        // Watch all namespaces CRB
        // https://strimzi.io/docs/operators/latest/deploying#deploying-cluster-operator-to-watch-whole-cluster-str
        if (ResourceUtils.getKubeResource(ClusterRoleBinding.class, Constants.STRIMZI_CO_NAME + "-namespaced") == null) {
            KubeResourceManager.get().createResourceWithoutWait(getStrimziWatchAllCrbs());
        }

        if (Environment.SKIP_STRIMZI_INSTALLATION || ResourceUtils.getKubeResource(Deployment.class, this.deploymentNamespace, this.deploymentName) != null) {
            LOGGER.warn("Skipping Strimzi installation. It is already installed or env to skip installation was set to `true`!");
            return;
        }

        LOGGER.info("Install Strimzi Cluster Operator in namespace: {}", this.deploymentNamespace);
        KubeResourceManager.get().createResourceWithoutWait(getBundleCrds());
        KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoles());
        KubeResourceManager.get().createResourceWithoutWait(getBundleServiceAccount());
        KubeResourceManager.get().createResourceWithoutWait(getBundleRoleBindings());
        KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoleBindings());
        KubeResourceManager.get().createResourceWithoutWait(getBundleConfigMap());
        KubeResourceManager.get().createResourceWithWait(getBundleDeployment());
    }

    private ClusterRoleBinding[] getStrimziWatchAllCrbs() {
        return new ClusterRoleBinding[]{
            new ClusterRoleBindingBuilder()
                .withNewMetadata()
                    .withName(Constants.STRIMZI_CO_NAME + "-namespaced")
                .endMetadata()
                .withNewRoleRef()
                    .withApiGroup(Constants.RBAC_AUTH_API_GROUP)
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(Constants.STRIMZI_CO_NAME + "-namespaced")
                .endRoleRef()
                .withSubjects(new SubjectBuilder()
                    .withKind(HasMetadata.getKind(ServiceAccount.class))
                    .withName(Constants.STRIMZI_CO_NAME)
                    .withNamespace(this.deploymentNamespace)
                    .build())
                .build(),
            new ClusterRoleBindingBuilder()
                .withNewMetadata()
                    .withName(Constants.STRIMZI_CO_NAME  + "-entity-operator")
                .endMetadata()
                .withNewRoleRef()
                    .withApiGroup(Constants.RBAC_AUTH_API_GROUP)
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(Constants.STRIMZI_CO_NAME  + "-entity-operator")
                .endRoleRef()
                .withSubjects(new SubjectBuilder()
                    .withKind(HasMetadata.getKind(ServiceAccount.class))
                    .withName(Constants.STRIMZI_CO_NAME)
                    .withNamespace(this.deploymentNamespace)
                    .build())
                .build(),
            new ClusterRoleBindingBuilder()
                .withNewMetadata()
                    .withName(Constants.STRIMZI_CO_NAME + "-watched")
                .endMetadata()
                .withNewRoleRef()
                    .withApiGroup(Constants.RBAC_AUTH_API_GROUP)
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(Constants.STRIMZI_CO_NAME + "-watched")
                .endRoleRef()
                .withSubjects(new SubjectBuilder()
                    .withKind(HasMetadata.getKind(ServiceAccount.class))
                    .withName(Constants.STRIMZI_CO_NAME)
                    .withNamespace(this.deploymentNamespace)
                    .build())
                .build()
        };
    }

    private ConfigMap getBundleConfigMap() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, ConfigMap.class))
            .map(r -> new ConfigMapBuilder(TestFrameUtils.configFromYaml(r, ConfigMap.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
                .withName(this.deploymentName)
            .endMetadata()
            .build())
            .toList()
            .get(0);
    }

    private ServiceAccount getBundleServiceAccount() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, ServiceAccount.class))
            .map(r -> new ServiceAccountBuilder(TestFrameUtils.configFromYaml(r, ServiceAccount.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
            .endMetadata()
            .build())
            .toList()
            .get(0);
    }

    private Deployment getBundleDeployment() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, Deployment.class))
            .map(r -> new DeploymentBuilder(TestFrameUtils.configFromYaml(r, Deployment.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
                .withName(this.deploymentName)
            .endMetadata()
            .editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .removeMatchingFromEnv(e -> e.getName().equals(Constants.STRIMZI_NAMESPACE_KEY))
                            .addNewEnv()
                                .withName(Constants.STRIMZI_NAMESPACE_KEY)
                                .withValue(Constants.STRIMZI_WATCH_ALL_NAMESPACES)
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build())
            .toList()
            .get(0);
    }

    private CustomResourceDefinition[] getBundleCrds() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, CustomResourceDefinition.class))
            .map(r -> new CustomResourceDefinitionBuilder(TestFrameUtils.configFromYaml(r, CustomResourceDefinition.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(CustomResourceDefinition[]::new);
    }

    private ClusterRole[] getBundleClusterRoles() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, ClusterRole.class))
            .map(r -> new ClusterRoleBuilder(TestFrameUtils.configFromYaml(r, ClusterRole.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }

    private RoleBinding[] getBundleRoleBindings() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, RoleBinding.class))
            .map(r -> new RoleBindingBuilder(TestFrameUtils.configFromYaml(r, RoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(RoleBinding[]::new);
    }

    private ClusterRoleBinding[] getBundleClusterRoleBindings() {
        return strimziResources.stream()
            .filter(content -> SetupUtils.isOfKind(content, ClusterRoleBinding.class))
            .map(r -> new ClusterRoleBindingBuilder(TestFrameUtils.configFromYaml(r, ClusterRoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(ClusterRoleBinding[]::new);
    }

    private List<String> getStrimziYamlFilesContent() {
        LOGGER.debug("Extracting Strimzi YAML file links from GitHub url: {}", Environment.STRIMZI_OPERATOR_VERSION);
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                // Github row locator of current dir
                List<String> yamlFileLinks = Jsoup.parse(new URL(resourceURL), 10_000)
                    .select("tr.react-directory-row div.react-directory-filename-cell a.Link--primary")
                    .eachAttr("href").stream()
                    .map(it -> SetupUtils.getYamlContent("https://raw.githubusercontent.com" + it.replace("/blob/", "/")))
                    .distinct()
                    .toList();

                if (!yamlFileLinks.isEmpty()) {
                    return yamlFileLinks;
                }
                LOGGER.warn("Attempt {}: Could not get GitHub Strimzi YAML file links, retrying in 3 seconds...", attempt + 1);
                Thread.sleep(3_000);
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Attempt {} failed: {}", attempt + 1, e.getMessage());
            }
        }
        throw new RuntimeException("Could not retrieve Strimzi CRs from GitHub after " + 10 + " attempts");
    }
}
