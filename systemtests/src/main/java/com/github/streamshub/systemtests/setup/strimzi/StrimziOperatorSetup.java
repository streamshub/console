package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.skodjob.kubetest4j.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private static final String CHART = "oci://quay.io/strimzi-helm/strimzi-kafka-operator";
    private final String deploymentNamespace;
    private final String deploymentName;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;
        LOGGER.debug("Prepared Strimzi Operator setup for deployment [{}] in namespace [{}]", deploymentName, deploymentNamespace);
    }

    public void install() {
        install(Environment.STRIMZI_OPERATOR_VERSION);
    }

    /**
     * Installs the Strimzi Cluster Operator at an explicit version, overriding the suite-wide
     * {@link Environment#STRIMZI_OPERATOR_VERSION} default.
     *
     * <p>Needed by tests that must pair a specific Strimzi version with a specific console-operator
     * version - e.g. an OLM upgrade test whose "before" console-operator version was built against an
     * older {@code io.strimzi:api} that only speaks a Kafka CRD version (such as {@code v1beta2}) that
     * newer Strimzi releases have stopped serving entirely.
     *
     * @param version the Strimzi Helm chart/operator version to install
     */
    // V1 not compatible with 0.12 console
    public void install(String version) {
        LOGGER.info("----------- Install Strimzi Cluster Operator -----------");
        if (Environment.SKIP_STRIMZI_INSTALLATION ||
            ResourceUtils.getKubeResource(Deployment.class, deploymentNamespace, deploymentName) != null) {
            LOGGER.warn("Strimzi Operator [{}] in namespace [{}] is already installed, or installation was skipped via SKIP_STRIMZI_INSTALLATION={}",
                deploymentName, deploymentNamespace, Environment.SKIP_STRIMZI_INSTALLATION);
            return;
        }

        // Helm never updates or removes already-installed CRDs on install/upgrade/uninstall (by design,
        // to avoid destroying data on a schema change). If a different Strimzi version was ever installed
        // on this cluster before - even if it was since uninstalled - its CRDs (and the API versions they
        // serve) are still there, and this Helm chart won't touch them. Installing this version's operator
        // on top of stale, mismatched CRDs leaves it unable to reconcile at all: it requests API versions
        // the leftover CRDs don't serve. We only reach this point once we've confirmed above that no
        // Strimzi deployment currently exists, so a fresh Helm install is about to happen regardless -
        // clearing any stale CRDs first guarantees they get reinstalled matching the version we're about
        // to deploy, whatever it is.
        deleteCrds();

        LOGGER.info("Installing Strimzi Operator [{}] version [{}] via Helm chart [{}] into namespace [{}]",
            deploymentName, version, CHART, deploymentNamespace);
        Helm.install(CHART)
            .withName(deploymentName)
            .withNamespace(deploymentNamespace)
            .withVersion(version)
            .set("watchAnyNamespace", true)
            .waitReady()
            .call();

        // Additional check that Strimzi deployment was installed
        LOGGER.debug("Waiting for Strimzi Operator deployment [{}] in namespace [{}] to become ready", deploymentName, deploymentNamespace);
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::uninstall));
        LOGGER.info("Strimzi Operator [{}] installation completed in namespace [{}]", deploymentName, deploymentNamespace);
    }

    public void uninstall() {
        LOGGER.info("----------- Uninstall Strimzi Cluster Operator -----------");
        LOGGER.info("Uninstalling Strimzi Operator [{}] from namespace [{}]", deploymentName, deploymentNamespace);
        // In case namespace is deleted before operator, ignore error that the release is not present (it's uninstalled)
        Helm.uninstall(deploymentName).ignoreNotFound().call();
    }

    /**
     * Deletes every Strimzi-owned CustomResourceDefinition currently on the cluster, identified by
     * Strimzi's own {@code strimzi.io/crd-install=true} label (applied to every CRD its Helm chart installs).
     *
     * <p><strong>Destructive:</strong> deleting a CRD cascades to delete every existing custom resource of
     * that kind, cluster-wide, in every namespace - not just this deployment's. Only safe to call when
     * nothing else on the cluster still needs its Kafka-family resources (Kafka, KafkaTopic, KafkaUser,
     * etc.) to exist, which holds here since this only runs immediately before a fresh Strimzi install.
     */
    private void deleteCrds() {
        LabelSelector strimziCrdSelector = new LabelSelectorBuilder().withMatchLabels(Map.of("strimzi.io/crd-install", "true")).build();
        List<CustomResourceDefinition> strimziCrds = ResourceUtils.listKubeResourcesByLabelSelector(CustomResourceDefinition.class, strimziCrdSelector);

        if (strimziCrds.isEmpty()) {
            LOGGER.debug("No existing Strimzi CustomResourceDefinitions found, nothing to delete");
            return;
        }

        LOGGER.warn("Deleting {} existing Strimzi CustomResourceDefinition(s) to clear any stale, version-mismatched CRDs a previously installed Strimzi may have left behind: {}",
            strimziCrds.size(), strimziCrds.stream().map(crd -> crd.getMetadata().getName()).toList());
        strimziCrds.forEach(crd -> ResourceUtils.getKubeResourceClient(CustomResourceDefinition.class).withName(crd.getMetadata().getName()).delete());
    }
}
