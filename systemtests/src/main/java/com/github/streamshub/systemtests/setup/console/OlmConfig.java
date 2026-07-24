package com.github.streamshub.systemtests.setup.console;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.OperatorSdkNotInstalledException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.upgrade.OlmVersionModificationData;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.zafarkhaja.semver.Version;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.operatorhub.packages.v1.ChannelEntry;
import io.fabric8.openshift.api.model.operatorhub.packages.v1.PackageChannel;
import io.fabric8.openshift.api.model.operatorhub.packages.v1.PackageManifest;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CatalogSource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CatalogSourceBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

import static org.awaitility.Awaitility.await;

public class OlmConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(OlmConfig.class);
    private static final Pattern CHANNEL_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");

    private final String olmAppBundlePrefix = Environment.CONSOLE_DEPLOYMENT_NAME;
    private final String packageName = Environment.CONSOLE_OLM_PACKAGE_NAME;
    private final String catalogSourceName = Environment.CONSOLE_OLM_CATALOG_SOURCE_NAME;
    private final String catalogSourceNamespace = Environment.CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE;
    private final String catalogSourceImage = Environment.CONSOLE_OLM_CATALOG_SOURCE_IMAGE;
    private final String subscriptionName = Constants.CONSOLE_OLM_SUBSCRIPTION_NAME;

    private String channelName = Environment.CONSOLE_OLM_CHANNEL_NAME;

    public OlmConfig(String namespace) {
        super(namespace);
        LOGGER.info("Console Operator will be installed using OLM");
        LOGGER.debug("OLM config: package='{}', channel='{}', catalogSource='{}/{}', namespace='{}'",
            packageName, channelName, catalogSourceNamespace, catalogSourceName, namespace);
        if (!ClusterUtils.isOcp() &&
            ResourceUtils.getKubeResource(CustomResourceDefinition.class, "subscriptions.operators.coreos.com") == null) {
            throw new OperatorSdkNotInstalledException("Operator SDK is not installed on the current cluster. Cannot install Console Operator using subscriptions");
        }
    }

    private void maybeCreateCatalogSource() {
        if (!catalogSourceImage.isEmpty() &&
            ResourceUtils.getKubeResource(CatalogSource.class, catalogSourceNamespace, catalogSourceName) == null) {
            LOGGER.info("Creating CatalogSource '{}' in namespace '{}' using image '{}'", catalogSourceName, catalogSourceNamespace, catalogSourceImage);
            KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmCatalogSource());
        }
    }

    @Override
    public void install() {

        if (!ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, deploymentName).isEmpty() &&
            !Environment.DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL) {
            LOGGER.info("Console Operator deployment '{}' already exists in namespace '{}', skipping OLM install", deploymentName, deploymentNamespace);
            return;
        }

        maybeCreateCatalogSource();

        LOGGER.info("Creating OLM OperatorGroup and Subscription '{}' for package '{}' in namespace '{}'", subscriptionName, packageName, deploymentNamespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmSubscription());

        LOGGER.info("Waiting for Console Operator deployment with prefix '{}' to become ready in namespace '{}'", olmAppBundlePrefix, deploymentNamespace);
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, olmAppBundlePrefix);

        // Get and set the deployment full name from known OLM bundle prefix
        deploymentName = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, olmAppBundlePrefix)
            .getFirst()
            .getMetadata()
            .getName();
        LOGGER.debug("Resolved Console Operator deployment name to '{}'", deploymentName);
    }

    @Override
    public void delete() {
        LOGGER.info("Deleting OLM OperatorGroup and Subscription '{}' in namespace '{}'", subscriptionName, deploymentNamespace);
        KubeResourceManager.get().deleteResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().deleteResourceWithWait(getOlmSubscription());
        List<Deployment> deploymentList = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, deploymentName);
        if (!deploymentList.isEmpty()) {
            // delete csv
            String csvFullName = ResourceUtils.listKubeResourcesByPrefix(ClusterServiceVersion.class, deploymentNamespace, packageName).getFirst().getMetadata().getName();
            LOGGER.debug("Deleting ClusterServiceVersion '{}' in namespace '{}'", csvFullName, deploymentNamespace);
            ClusterServiceVersion csv = ResourceUtils.getKubeResource(ClusterServiceVersion.class, deploymentNamespace, csvFullName);
            KubeResourceManager.get().deleteResourceWithWait(csv);
            LOGGER.info("Deleting Console Operator deployment '{}' in namespace '{}'", deploymentList.getFirst().getMetadata().getName(), deploymentNamespace);
            KubeResourceManager.get().deleteResourceWithWait(deploymentList.getFirst());
        } else {
            LOGGER.debug("No Console Operator deployment found with prefix '{}' in namespace '{}', nothing to delete", deploymentName, deploymentNamespace);
        }
    }

    public OlmVersionModificationData getUpgradeData() {
        maybeCreateCatalogSource();

        String newVersion = Environment.getConsoleOperatorVersion();

        String[] newVersionElements = newVersion.split("[.-]");
        var newChannelSemVer = Version.of(
                Long.valueOf(newVersionElements[0]),
                Long.valueOf(newVersionElements[1])
        );

        var manifestClient = KubeResourceManager.get()
                .kubeClient()
                .getOpenShiftClient()
                .operatorHub()
                .packageManifests()
                .inNamespace(Environment.CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE)
                .withName(Environment.CONSOLE_OLM_PACKAGE_NAME);

        var packageManifest = await("PackageManifest contains operator version")
                .atMost(10, TimeUnit.SECONDS)
                .ignoreException(NoSuchElementException.class)
                .until(
                    manifestClient::get,
                    pm -> getPackageChannel(pm, newChannelSemVer) != null
                );

        var newPackageChannel = getPackageChannel(packageManifest, newChannelSemVer);
        var oldPackageChannel = getPreviousPackageChannel(packageManifest, newChannelSemVer);

        OlmVersionModificationData olmUpgradeData = new OlmVersionModificationData();

        olmUpgradeData.setOldOlmChannel(oldPackageChannel.getName());
        olmUpgradeData.setOldOperatorVersion(getLatestVersion(oldPackageChannel));

        olmUpgradeData.setNewOlmChannel(newPackageChannel.getName());
        olmUpgradeData.setNewOperatorVersion(newVersion);

        LOGGER.info("Loaded OLM upgrade data: operator version {} -> {}, channel {} -> {}",
                olmUpgradeData.getOldOperatorVersion(), olmUpgradeData.getNewOperatorVersion(),
                olmUpgradeData.getOldOlmChannel(), olmUpgradeData.getNewOlmChannel());

        return olmUpgradeData;
    }

    /**
     * Find a channel in the package manifest with a name that contains the channelVersion
     * in the name.
     */
    private PackageChannel getPackageChannel(PackageManifest manifest, Version channelVersion) {
        return manifest.getStatus()
            .getChannels()
            .stream()
            .filter(c -> channelVersion(c.getName()).map(channelVersion::equals).orElse(false))
            .findFirst()
            .orElseThrow();
    }

    /**
     * Find a channel in the package manifest with a name that contains the latest version (according
     * to SemVer) that is lower than channelVersion.
     */
    private PackageChannel getPreviousPackageChannel(PackageManifest manifest, Version channelVersion) {
        return manifest.getStatus()
            .getChannels()
            .stream()
            .map(c -> channelVersion(c.getName())
                .filter(Predicate.not(channelVersion::isLowerThanOrEquivalentTo))
                .map(otherChannelSemVer -> Map.entry(otherChannelSemVer, c))
                .orElse(null))
            .filter(Objects::nonNull)
            .max(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue)
            .orElseThrow();
    }

    private Optional<Version> channelVersion(String channelName) {
        Matcher m = CHANNEL_VERSION_PATTERN.matcher(channelName);

        if (m.find()) {
            return Optional.of(Version.of(
                    Long.valueOf(m.group(1)),
                    Long.valueOf(m.group(2))
            ));
        }

        return Optional.empty();
    }

    private String getLatestVersion(PackageChannel packageChannel) {
        return packageChannel.getEntries()
            .stream()
            .max(Comparator.comparing(e -> Version.parse(e.getVersion())))
            .map(ChannelEntry::getVersion)
            .orElseThrow();
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
     * Bumps the Subscription's channel to trigger an in-place OLM upgrade of the Console Operator,
     * without going through {@link #install()}'s "already installed, skip" guard.
     *
     * <p>{@link #install()} intentionally no-ops when a Deployment already exists and
     * {@link Environment#DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL} is {@code false} - that guard exists
     * purely as a debugging convenience (skip redeploying between local runs) and has nothing to do
     * with a deliberate upgrade test switching channels on an existing Subscription. Patching the
     * Subscription directly here sidesteps that guard entirely: OLM picks up the channel change,
     * resolves the new CSV and rolls the Deployment on its own.
     *
     * @param channelName the OLM channel to switch the Subscription to
     */
    public void updateChannel(String channelName) {
        this.channelName = channelName;
        LOGGER.info("Bumping Console Operator Subscription '{}' to channel '{}' in namespace '{}'", subscriptionName, channelName, deploymentNamespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmSubscription());
    }

    private CatalogSource getOlmCatalogSource() {
        return new CatalogSourceBuilder()
            .withNewMetadata()
                .withName(catalogSourceName)
                .withNamespace(catalogSourceNamespace)
            .endMetadata()
            .withNewSpec()
                .withDisplayName("StreamsHub")
                .withImage(catalogSourceImage)
                .withPublisher("StreamsHub")
                .withSourceType("grpc")
            .endSpec()
            .build();
    }

    private Subscription getOlmSubscription() {
        return new SubscriptionBuilder()
            .editMetadata()
                .withNamespace(deploymentNamespace)
                .withName(subscriptionName)
            .endMetadata()
            .editSpec()
                .withName(packageName)
                .withSource(catalogSourceName)
                .withInstallPlanApproval("Automatic")
                .withChannel(channelName)
                .withSourceNamespace(catalogSourceNamespace)
            .endSpec()
            .build();
    }

    private OperatorGroup getOlmOperatorGroup() {
        return new OperatorGroupBuilder()
            .withNewMetadata()
                .withName("streamshub-operators")
                .withNamespace(deploymentNamespace)
            .endMetadata()
            .withNewSpec()
                .withTargetNamespaces(Collections.emptyList())
                .withUpgradeStrategy("Default")
            .endSpec()
            .build();
    }
}
