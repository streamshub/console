package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBroker;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class KafkaUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaUtils.class);

    private KafkaUtils() {}

    public static String getPlainBootstrapAddress(String clusterName) {
        return getBootstrapServiceName(clusterName) + ":9092";
    }

    public static String getPlainScramShaBootstrapAddress(String clusterName) {
        return getBootstrapServiceName(clusterName) + ":9095";
    }

    public static String getBootstrapServiceName(String clusterName) {
        return clusterName + "-kafka-bootstrap";
    }

    /**
     * Adds an annotation to a Kafka resource in the specified namespace.
     *
     * <p>If the annotation key does not exist or its value is different from the provided one,
     * this method updates the Kafka custom resource metadata with the new annotation.</p>
     * <p>If {@code wait} is {@code true}, it waits until the annotation with the expected value appears on the Kafka resource.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka resource is located
     * @param kafkaName the name of the Kafka custom resource
     * @param annoKey the annotation key to add or update
     * @param annoVal the annotation value to set
     * @param wait whether to wait for the annotation to be applied
     */
    public static void addAnnotation(String namespace, String kafkaName, String annoKey, String annoVal, boolean wait) {
        LOGGER.info("Adding annotation: {}:{} to Kafka: {}/{}", annoKey, annoVal, namespace, kafkaName);
        Kafka oldKafka = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName);
        Map<String, String> anno = oldKafka.getMetadata().getAnnotations();
        if (!java.util.Objects.equals(anno.get(annoKey), annoVal)) {
            anno.put(annoKey, annoVal);
            KubeResourceManager.get().createOrUpdateResourceWithWait(
                new KafkaBuilder(oldKafka)
                .editMetadata()
                    .withAnnotations(anno)
                .endMetadata()
                .build()
            );
        }

        if (wait) {
            WaitUtils.waitForKafkaHasAnnotationWithValue(namespace, kafkaName, annoKey, annoVal);
        }
    }

    /**
     * Removes a specific annotation from a Kafka resource in the given namespace.
     *
     * <p>If the Kafka resource contains the specified annotation key, it is removed from the metadata.</p>
     * <p>If {@code wait} is {@code true}, the method waits until the annotation is no longer present on the resource.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka resource is located
     * @param kafkaName the name of the Kafka custom resource
     * @param annoKey the annotation key to remove
     * @param wait whether to wait for the annotation to be removed
     */
    public static void removeAnnotation(String namespace, String kafkaName, String annoKey, boolean wait) {
        LOGGER.info("Removing annotation: {} from Kafka: {}/{}", annoKey, namespace, kafkaName);
        Kafka oldKafka = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName);
        Map<String, String> anno = oldKafka.getMetadata().getAnnotations();
        if (anno.containsKey(annoKey)) {
            anno.remove(annoKey);
            KubeResourceManager.get().createOrUpdateResourceWithWait(
                new KafkaBuilder(oldKafka)
                .editMetadata()
                    .withAnnotations(anno)
                .endMetadata()
                .build()
            );
        }

        if (wait) {
            WaitUtils.waitForKafkaHasNoAnnotationWithKey(namespace, kafkaName, annoKey);
        }
    }

    /**
     * Calculates the node IDs for newly added Kafka broker replicas
     * when scaling a Kafka cluster managed by the Strimzi operator.
     *
     * <p>This method inspects the current Kafka broker pods to determine
     * existing node IDs, then computes a list of new node IDs for brokers
     * that need to be added to reach the desired replica count.</p>
     *
     * <p>This utility is used during broker scaling operations
     * to ensure consistent and gap free node ID allocation.</p>
     *
     * @param namespace the Kubernetes namespace of the Kafka cluster
     * @param kafkaName the name of the Kafka cluster
     * @param existingReplicas the current number of broker replicas
     * @param desiredReplicas the desired number of broker replicas
     * @return a list of new node IDs to assign to added brokers
     */
    public static List<Integer> getNewNodePoolNodeIds(String namespace, String kafkaName, int existingReplicas, int desiredReplicas) {
        // Get existing pod names from all Kafka nodepools
        List<String> podNames = ResourceUtils
            .listKubeResourcesByLabelSelector(Pod.class, namespace, Labels.getKafkaPodLabelSelector(kafkaName))
            .stream()
            .map(p -> p.getMetadata().getName())
            .toList();

        // Extract existing node IDs from pod name suffixes
        List<Integer> nodeIds = podNames.stream()
            .map(name -> {
                int lastDashIndex = name.lastIndexOf('-');
                if (lastDashIndex == -1 || lastDashIndex == name.length() - 1) {
                    throw new IllegalArgumentException("Invalid Strimzi Kafka pod name structure: " + name);
                }
                // return number after the last dash - by strimzi naming standard this should be nodeId
                return Integer.parseInt(name.substring(lastDashIndex + 1));
            })
            .sorted()
            .toList();

        // Compute the current maximum node ID (or -1 if none)
        int maxId = nodeIds.isEmpty() ? -1 : Collections.max(nodeIds);

        // Example: newlyAdded = how many replicas to add (e.g. 4 new nodes)
        int newlyAdded = desiredReplicas - existingReplicas;

        // Compute new IDs continuing after the current max
        return IntStream.rangeClosed(maxId + 1, maxId + newlyAdded)
            .boxed()
            .toList();
    }

    /**
     * Scales the number of Kafka broker replicas for a given Kafka cluster within a namespace.
     *
     * <p>This method dynamically updates both the {@link Kafka} and {@link KafkaNodePool}
     * custom resources to reflect the desired broker replica count, adjusting broker
     * listener configurations as needed. It ensures the cluster reaches the target state
     * by waiting until all expected broker pods are ready and stable.</p>
     *
     * <p>This utility is commonly used to validate dynamic scaling
     * behavior of Kafka brokers managed by the Strimzi operator mainly for ingress.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka cluster is deployed
     * @param kafkaName the name of the Kafka cluster to scale
     * @param scaledBrokersCount the desired number of broker replicas
     */
    public static void scaleBrokerReplicas(String namespace, String kafkaName, int scaledBrokersCount) {
        LOGGER.info("Scale Kafka broker replicas to {}", scaledBrokersCount);

        int existingBrokerReplicas = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespace, Labels.getKnpBrokerLabelSelector(kafkaName)).size();
        if (existingBrokerReplicas == scaledBrokersCount) {
            return;
        }

        // Add each new broker host with broker ID into the config
        GenericKafkaListener currentListenersBrokerConfig = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName)
            .getSpec()
            .getKafka()
            .getListeners()
            .stream()
            .filter(listener -> listener.getName().equals(Constants.SECURE_LISTENER_NAME))
            .toList()
            .get(0);


        List<GenericKafkaListenerConfigurationBroker> newBrokerConfigList = new ArrayList<>();
        String hashedNamespace = Utils.hashStub(namespace);

        if (existingBrokerReplicas < scaledBrokersCount) {
            // Add existing broker listener
            newBrokerConfigList.addAll(currentListenersBrokerConfig.getConfiguration().getBrokers());

            for (int nodeId : getNewNodePoolNodeIds(namespace, kafkaName, existingBrokerReplicas, scaledBrokersCount)) {
                LOGGER.debug("Add Kafka listener to broker node ID {}", nodeId);
                newBrokerConfigList.add(new GenericKafkaListenerConfigurationBrokerBuilder(
                    new GenericKafkaListenerConfigurationBrokerBuilder()
                        .withBroker(nodeId)
                        .withHost(String.join(".", "broker-" + nodeId, hashedNamespace, kafkaName, ClusterUtils.getClusterDomain()))
                        .build()
                    ).build());
            }
        } else {
            newBrokerConfigList.addAll(currentListenersBrokerConfig.getConfiguration().getBrokers().subList(0, scaledBrokersCount));
        }

        // Process new broker config listener list
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaBuilder(ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName))
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> l.getName().equals(Constants.SECURE_LISTENER_NAME))
                            .editConfiguration()
                                .withBrokers(newBrokerConfigList)
                            .endConfiguration()
                        .endListener()
                    .endKafka()
                .endSpec()
                .build());

        // Edit KNP
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            new KafkaNodePoolBuilder(ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName)))
                .editSpec()
                    .withReplicas(scaledBrokersCount)
                .endSpec()
                .build());

        // Wait for change of replicas in config and also pods
        WaitUtils.waitForKafkaBrokerNodePoolReplicasInSpec(namespace, kafkaName, scaledBrokersCount);
        WaitUtils.waitForPodsReadyAndStable(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), scaledBrokersCount, true);
        LOGGER.info("Kafka broker replicas successfully scaled to {}", scaledBrokersCount);
    }
}
