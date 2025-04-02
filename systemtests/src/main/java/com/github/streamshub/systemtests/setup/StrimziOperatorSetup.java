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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);

    private final List<File> strimziResources = fetchStrimziYamlFiles();
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

    public void teardown() {
        LOGGER.info("----------- Teardown Strimzi Cluster Operator -----------");
        if (Environment.SKIP_STRIMZI_INSTALLATION || ResourceUtils.getKubeResource(Deployment.class, this.deploymentNamespace, this.deploymentName) == null) {
            LOGGER.warn("Skipping Strimzi deletion. It is already deleted or installation was skipped!");
            return;
        }

        // Watch all namespaces CRB
        KubeResourceManager.get().deleteResource(getStrimziWatchAllCrbs());
        KubeResourceManager.get().deleteResource(getBundleCrds());
        KubeResourceManager.get().deleteResource(getBundleClusterRoles());
        KubeResourceManager.get().deleteResource(getBundleServiceAccount());
        KubeResourceManager.get().deleteResource(getBundleRoleBindings());
        KubeResourceManager.get().deleteResource(getBundleClusterRoleBindings());
        KubeResourceManager.get().deleteResource(getBundleConfigMap());
        KubeResourceManager.get().deleteResource(getBundleDeployment());
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
        return new ConfigMapBuilder(TestFrameUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().startsWith("050-ConfigMap")).toList().get(0), ConfigMap.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
                .withName(this.deploymentName)
            .endMetadata()
            .build();
    }

    private ServiceAccount getBundleServiceAccount() {
        return new ServiceAccountBuilder(TestFrameUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().startsWith("010-ServiceAccount")).toList().get(0), ServiceAccount.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
            .endMetadata()
            .build();
    }

    private Deployment getBundleDeployment() {
        return new DeploymentBuilder(TestFrameUtils.configFromYaml(strimziResources.stream()
            .filter(file -> file.getName().startsWith("060-Deployment")).toList().get(0), Deployment.class))
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

    private CustomResourceDefinition[] getBundleCrds() {
        return strimziResources.stream().filter(file -> file.getName().contains("-Crd-"))
            .map(crd -> new CustomResourceDefinitionBuilder(TestFrameUtils.configFromYaml(crd, CustomResourceDefinition.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(CustomResourceDefinition[]::new);
    }

    private ClusterRole[] getBundleClusterRoles() {
        return strimziResources.stream().filter(file -> file.getName().contains("-ClusterRole-"))
            .map(file -> new ClusterRoleBuilder(TestFrameUtils.configFromYaml(file, ClusterRole.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }

    private RoleBinding[] getBundleRoleBindings() {
        return strimziResources.stream().filter(file -> file.getName().contains("-RoleBinding-"))
            .map(rb -> new RoleBindingBuilder(TestFrameUtils.configFromYaml(rb, RoleBinding.class))
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
        return strimziResources.stream().filter(file -> file.getName().contains("-ClusterRoleBinding-"))
            .map(crb -> new ClusterRoleBindingBuilder(TestFrameUtils.configFromYaml(crb, ClusterRoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(ClusterRoleBinding[]::new);
    }

    private List<File> fetchStrimziYamlFiles() {
        List<File> yamlFiles = new ArrayList<>();
        for (String fileUrl : extractYamlFileLinksFromGithubDir()) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1).replace(".yaml", "");
            LOGGER.debug("Fetching Strimzi YAML file: {}", fileName);
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile(fileName + "-tmp_", ".yaml");
                Files.write(tempFile, SetupUtils.getYamlContentFromUrl(fileUrl).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            yamlFiles.add(tempFile.toFile());
        }
        return yamlFiles;
    }

    private List<String> extractYamlFileLinksFromGithubDir() {
        LOGGER.debug("Extracting Strimzi YAML file links from GitHub url: {}", Environment.STRIMZI_OPERATOR_CRDS_URL);
        int maxRetries = 10;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                BufferedReader htmlWithLinksFromDir = new BufferedReader(
                    new InputStreamReader(new URL(Environment.STRIMZI_OPERATOR_CRDS_URL).openStream()));

                StringBuilder content = new StringBuilder();
                String inputLine;

                while ((inputLine = htmlWithLinksFromDir.readLine()) != null) {
                    content.append(inputLine);
                }

                htmlWithLinksFromDir.close();

                List<String> yamlFileLinks = new ArrayList<>();
                Matcher matcher = Pattern.compile("href=\"(\\/[a-zA-Z0-9\\.\\/\\-]+\\/.{3}[a-zA-Z\\-]+\\d?\\.yaml)\"")
                    .matcher(content.toString());

                while (matcher.find()) {
                    final String rawUrl = "https://raw.githubusercontent.com" + matcher.group(1)
                        .replace("/blob/", "/");
                    if (!yamlFileLinks.contains(rawUrl)) {
                        yamlFileLinks.add(rawUrl);
                    }
                }

                if (!yamlFileLinks.isEmpty()) {
                    return yamlFileLinks;
                }

                LOGGER.warn("Attempt {}: Could not get GitHub Strimzi YAML file links, retrying in 3 seconds...", attempt + 1);
                Thread.sleep(3_000);
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Attempt {} failed: {}", attempt + 1, e.getMessage());
            }

            attempt++;
        }
        throw new RuntimeException("Could not retrieve Strimzi CRs from GitHub after " + maxRetries + " attempts");
    }
}
