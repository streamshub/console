package com.github.streamshub.systemtests.setup;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.resources.ClusterRoleBindingUtils;
import com.github.streamshub.systemtests.utils.resources.DeploymentUtils;
import com.github.streamshub.systemtests.utils.resources.OperatorGroupUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
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
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.streamshub.systemtests.utils.SetupUtils.fetchStrimziYamlFiles;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private static final KubeResourceManager RESOURCE_MANAGER = KubeResourceManager.getInstance();
    private final String deploymentNamespace;
    private final String deploymentName;
    private final Deployment strimziDeployment;
    private final ClusterRole[] strimziClusterRoles;
    private final CustomResourceDefinition[] strimziCrds;
    private final ClusterRoleBinding[] strimziClusterRoleBindings;
    private final RoleBinding[] strimziRoleBindings;
    private final ServiceAccount strimziServiceAccount;
    private final ConfigMap strimziConfigMap;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;

        List<File> strimziResources = fetchStrimziYamlFiles();
        this.strimziDeployment = getBundleDeployment(strimziResources);
        this.strimziCrds = getBundleCrds(strimziResources);
        this.strimziClusterRoles = getBundleClusterRoles(strimziResources);
        this.strimziRoleBindings = getBundleRoleBindings(strimziResources);
        this.strimziClusterRoleBindings = getBundleClusterRoleBindings(strimziResources);
        this.strimziServiceAccount = getBundleServiceAccount(strimziResources);
        this.strimziConfigMap = getBundleConfigMap(strimziResources);
    }

    public void setup() {
        LOGGER.info("----------- Setup Strimzi Operator -----------");
        createClusterWideRoleBindings();
        if (Environment.SKIP_STRIMZI_INSTALLATION || DeploymentUtils.getDeployment(this.deploymentNamespace, this.deploymentName) != null) {
            LOGGER.warn("Skipping Strimzi installation. It is already installed or installation was skipped!");
            return;
        }

        LOGGER.info("Install Strimzi Cluster Operator in namespace: {}", this.deploymentNamespace);
        if (OperatorGroupUtils.listOperatorGroupByName(this.deploymentNamespace, Constants.CONSOLE_OPERATOR_GROUP_NAME).isEmpty()) {
            RESOURCE_MANAGER.createResourceWithoutWait(getStrimziOperatorGroup());
        }
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziCrds);
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziClusterRoles);
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziServiceAccount);
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziRoleBindings);
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziClusterRoleBindings);
        RESOURCE_MANAGER.createResourceWithoutWait(this.strimziConfigMap);
        RESOURCE_MANAGER.createResourceWithWait(this.strimziDeployment);
    }


    public void teardown() {
        LOGGER.info("----------- Teardown Strimzi Cluster Operator -----------");
        if (Environment.SKIP_STRIMZI_INSTALLATION || DeploymentUtils.getDeployment(this.deploymentNamespace, this.deploymentName) == null) {
            LOGGER.warn("Skipping Strimzi deletion. It is already deleted or installation was skipped!");
            return;
        }

        deleteClusterWideRoleBindings();
        RESOURCE_MANAGER.deleteResource(this.strimziCrds);
        RESOURCE_MANAGER.deleteResource(this.strimziClusterRoles);
        RESOURCE_MANAGER.deleteResource(this.strimziServiceAccount);
        RESOURCE_MANAGER.deleteResource(this.strimziRoleBindings);
        RESOURCE_MANAGER.deleteResource(this.strimziClusterRoleBindings);
        RESOURCE_MANAGER.deleteResource(this.strimziConfigMap);
        RESOURCE_MANAGER.deleteResource(this.strimziDeployment);
    }

    private void createClusterWideRoleBindings() {
        LOGGER.info("Creating ClusterWideRoleBindings in namespace: {}", this.deploymentNamespace);
        // Create ClusterRoleBindings that grant cluster-wide access to all OpenShift projects
        for (ClusterRoleBinding clusterRoleBinding: clusterRoleBindingsForAllNamespaces()) {
            if (ClusterRoleBindingUtils.getCrb(clusterRoleBinding.getMetadata().getName()) == null) {
                LOGGER.info("Creating CRB: {}", clusterRoleBinding.getMetadata().getName());
                RESOURCE_MANAGER.createResourceWithWait(clusterRoleBinding);
            } else {
                LOGGER.warn("ClusterRoleBinding already present on cluster {}", clusterRoleBinding.getMetadata().getName());
            }
        }
    }

    private void deleteClusterWideRoleBindings() {
        LOGGER.info("Deleting ClusterWideRoleBindings in namespace: {}", this.deploymentNamespace);
        for (ClusterRoleBinding clusterRoleBinding : clusterRoleBindingsForAllNamespaces()) {
            if (ClusterRoleBindingUtils.getCrb(clusterRoleBinding.getMetadata().getName()) != null) {
                LOGGER.info("Deleting CRB: {}", clusterRoleBinding.getMetadata().getName());
                RESOURCE_MANAGER.deleteResource(clusterRoleBinding);
            } else {
                LOGGER.warn("ClusterRoleBinding {} already deleted", clusterRoleBinding.getMetadata().getName());
            }
        }
    }

    private List<ClusterRoleBinding> clusterRoleBindingsForAllNamespaces() {
        LOGGER.debug("Return list of CRB for watching all namespaces");
        return Arrays.asList(
            getClusterOperatorNamespacedCrb(),
            getClusterOperatorEntityOperatorCrb(),
            getClusterOperatorWatchedCrb()
        );
    }

    private ClusterRoleBinding getClusterOperatorNamespacedCrb() {
        return new ClusterRoleBindingBuilder()
            .withNewMetadata()
                .withName(Constants.STRIMZI_CO_NAME + "-namespaced")
            .endMetadata()
            .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(Constants.STRIMZI_CO_NAME + "-namespaced")
            .endRoleRef()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(Constants.STRIMZI_CO_NAME)
                .withNamespace(this.deploymentNamespace)
                .build())
            .build();
    }

    private ClusterRoleBinding getClusterOperatorEntityOperatorCrb() {
        return new ClusterRoleBindingBuilder()
            .withNewMetadata()
                .withName(Constants.STRIMZI_CO_NAME  + "-entity-operator")
            .endMetadata()
            .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(Constants.STRIMZI_CO_NAME  + "-entity-operator")
            .endRoleRef()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(Constants.STRIMZI_CO_NAME)
                .withNamespace(this.deploymentNamespace)
                .build())
            .build();
    }

    private ClusterRoleBinding getClusterOperatorWatchedCrb() {
        return new ClusterRoleBindingBuilder()
            .withNewMetadata()
                .withName(Constants.STRIMZI_CO_NAME + "-watched")
            .endMetadata()
            .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(Constants.STRIMZI_CO_NAME + "-watched")
            .endRoleRef()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(Constants.STRIMZI_CO_NAME)
                .withNamespace(this.deploymentNamespace)
                .build())
            .build();
    }

    private OperatorGroup getStrimziOperatorGroup() {
        return new OperatorGroupBuilder()
            .editOrNewMetadata()
                .withName(Constants.CONSOLE_OPERATOR_GROUP_NAME)
                .withNamespace(this.deploymentNamespace)
                .withLabels(Collections.singletonMap("app", Constants.STRIMZI_NAME))
            .endMetadata()
            .build();
    }

    private ConfigMap getBundleConfigMap(List<File> strimziResources) {
        return new ConfigMapBuilder(SetupUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().matches(".*050-ConfigMap-.*\\.yaml")).toList().get(0), ConfigMap.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
                .withName(this.deploymentName)
            .endMetadata()
            .build();
    }

    private ServiceAccount getBundleServiceAccount(List<File> strimziResources) {
        return new ServiceAccountBuilder(SetupUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().matches(".*010-ServiceAccount-.*\\.yaml")).toList().get(0), ServiceAccount.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
            .endMetadata()
            .build();
    }

    private Deployment getBundleDeployment(List<File> strimziResources) {
        return new DeploymentBuilder(SetupUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().matches(".*060-Deployment-.*\\.yaml")).toList().get(0), Deployment.class))
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
            .build();
    }

    private CustomResourceDefinition[] getBundleCrds(List<File> strimziResources) {
        return strimziResources.stream().filter(file -> file.getName().matches(".*-Crd-.*\\.yaml"))
            .map(crd -> new CustomResourceDefinitionBuilder(SetupUtils.configFromYaml(crd, CustomResourceDefinition.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(CustomResourceDefinition[]::new);
    }

    private ClusterRole[] getBundleClusterRoles(List<File> strimziResources) {
        return strimziResources.stream().filter(file -> file.getName().matches(".*-ClusterRole-.*\\.yaml"))
            .map(file -> new ClusterRoleBuilder(SetupUtils.configFromYaml(file, ClusterRole.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }


    private RoleBinding[] getBundleRoleBindings(List<File> strimziResources) {
        return strimziResources.stream().filter(file -> file.getName().matches(".*-RoleBinding-.*\\.yaml"))
            .map(rb -> new RoleBindingBuilder(SetupUtils.configFromYaml(rb, RoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(RoleBinding[]::new);
    }


    private ClusterRoleBinding[] getBundleClusterRoleBindings(List<File> strimziResources) {
        return strimziResources.stream().filter(file -> file.getName().matches(".*-ClusterRoleBinding-.*\\.yaml"))
            .map(crb -> new ClusterRoleBindingBuilder(SetupUtils.configFromYaml(crb, ClusterRoleBinding.class))
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
