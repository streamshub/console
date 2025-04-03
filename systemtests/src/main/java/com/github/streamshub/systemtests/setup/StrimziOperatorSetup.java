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

import java.util.Arrays;
import java.util.List;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private final List<String> strimziResources;
    private final String deploymentNamespace;
    private final String deploymentName;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;

        this.strimziResources = Arrays.stream(SetupUtils.getYamlContent(String.format(
            "https://github.com/strimzi/strimzi-kafka-operator/releases/download/%s/strimzi-cluster-operator-%s.yaml",
            Environment.STRIMZI_OPERATOR_VERSION, Environment.STRIMZI_OPERATOR_VERSION))
            .split("---"))
            .toList();
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
}
