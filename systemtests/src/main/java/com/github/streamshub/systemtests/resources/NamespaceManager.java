package com.github.streamshub.systemtests.resources;

import com.github.streamshub.systemtests.logs.CollectorElement;
import io.fabric8.kubernetes.api.model.NamespaceStatus;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NamespaceManager {

    private static final Logger LOGGER = LogManager.getLogger(NamespaceManager.class);
    private static NamespaceManager instance;
    private final static Map<CollectorElement, Set<String>> MAP_WITH_SUITE_NAMESPACES = new HashMap<>();

    public static synchronized NamespaceManager getInstance() {
        if (instance == null) {
            instance = new NamespaceManager();
        }
        return instance;
    }

    private synchronized void addNamespaceToSet(String namespaceName, CollectorElement collectorElement) {
        if (MAP_WITH_SUITE_NAMESPACES.containsKey(collectorElement)) {
            Set<String> testSuiteNamespaces = MAP_WITH_SUITE_NAMESPACES.get(collectorElement);
            testSuiteNamespaces.add(namespaceName);
            MAP_WITH_SUITE_NAMESPACES.put(collectorElement, testSuiteNamespaces);
        } else {
            // test-suite is new
            MAP_WITH_SUITE_NAMESPACES.put(collectorElement, new HashSet<>(Set.of(namespaceName)));
        }

        LOGGER.trace("SUITE_NAMESPACE_MAP: {}", MAP_WITH_SUITE_NAMESPACES);
    }

    private synchronized void removeNamespaceFromSet(String namespaceName, CollectorElement collectorElement) {
        Set<String> testSuiteNamespaces = new HashSet<>(MAP_WITH_SUITE_NAMESPACES.get(collectorElement));
        testSuiteNamespaces.remove(namespaceName);

        MAP_WITH_SUITE_NAMESPACES.put(collectorElement, testSuiteNamespaces);

        LOGGER.trace("SUITE_NAMESPACE_MAP after deletion: {}", MAP_WITH_SUITE_NAMESPACES);
    }

    public void createNamespace(String namespaceName) {
        LOGGER.info("Creating Namespace: {}", namespaceName);
        // TODO:nextPR
        //NamespaceUtils.createNamespace(namespaceName);
    }

    public void createNamespaceWithWait(String namespaceName) {
        createNamespace(namespaceName);
        waitForNamespaceCreation(namespaceName);
    }

    public void createNamespaceAndAddToSet(String namespaceName, CollectorElement collectorElement) {
        createNamespaceWithWait(namespaceName);

        if (collectorElement != null) {
            addNamespaceToSet(namespaceName, collectorElement);
        }
    }

    public void createNamespaceAndPrepare(String namespaceName) {
        final String testSuiteName = KubeResourceManager.getTestContext().getRequiredTestClass().getName();
        final String testCaseName = KubeResourceManager.getTestContext().getTestMethod().orElse(null) == null ?
            "" : KubeResourceManager.getTestContext().getRequiredTestMethod().getName();

        createNamespaceAndPrepare(namespaceName, new CollectorElement(testSuiteName, testCaseName));
    }

    public void createNamespaceAndPrepare(String namespaceName, CollectorElement collectorElement) {
        createNamespaceAndAddToSet(namespaceName, collectorElement);
        // TODO:nextPR
        //NamespaceResource.copyImagePullSecrets(namespaceName);
    }

    public void deleteNamespace(String namespaceName) {
        LOGGER.info("Deleting Namespace: {}", namespaceName);
        // TODO:nextPR
        //NamespaceResource.deleteNamespace(namespaceName);
    }

    public void deleteNamespaceWithWait(String namespaceName) {
        deleteNamespace(namespaceName);
        waitForNamespaceDeletion(namespaceName);
    }

    public void deleteNamespaceWithWaitAndRemoveFromSet(String namespaceName, CollectorElement collectorElement) {
        deleteNamespaceWithWait(namespaceName);

        if (collectorElement != null) {
            removeNamespaceFromSet(namespaceName, collectorElement);
        }
    }

    public void deleteAllNamespacesFromSet() {
        MAP_WITH_SUITE_NAMESPACES.values()
            .forEach(setOfNamespaces ->
                setOfNamespaces.parallelStream()
                    .forEach(this::deleteNamespaceWithWait));

        MAP_WITH_SUITE_NAMESPACES.clear();
    }


    private void waitForNamespaceDeletion(String namespaceName) {
        // TODO:nextPR
    }

    private void waitForNamespaceCreation(String namespaceName) {
        // TODO:nextPR
    }

    private boolean isNamespaceDeletionStuckOnFinalizers(NamespaceStatus namespaceStatus) {
        return namespaceStatus != null
            && namespaceStatus.getConditions() != null
            && namespaceStatus.getConditions().stream().anyMatch(condition -> condition.getReason().contains("SomeFinalizersRemain"));
    }

    public static Map<CollectorElement, Set<String>> getMapWithSuiteNamespaces() {
        return MAP_WITH_SUITE_NAMESPACES;
    }

    public List<String> getListOfNamespacesForTestClassAndTestCase(String testClass, String testCase) {
        List<String> namespaces = new ArrayList<>();

        if (!getMapWithSuiteNamespaces().isEmpty()) {
            namespaces.addAll(getMapWithSuiteNamespaces().get(new CollectorElement(testClass)));
        } else {
            // TODO:nextPR
            //  namespaces.add(Constants.CO_NAMESPACE);
        }

        if (testCase != null) {
            Set<String> namespacesForTestCase = getMapWithSuiteNamespaces().get(new CollectorElement(testClass, testCase));

            if (namespacesForTestCase != null) {
                namespaces.addAll(getMapWithSuiteNamespaces().get(new CollectorElement(testClass, testCase)));
            }
        }

        return namespaces;
    }
}
