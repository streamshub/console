package com.github.streamshub.systemtests.users;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.KafkaUsersPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testutils.KafkaUserTestUtils;
import com.microsoft.playwright.Locator;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthorizationSimple;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import io.strimzi.api.kafka.model.user.acl.AclRule;
import io.strimzi.api.kafka.model.user.acl.AclRuleClusterResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleGroupResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTopicResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTransactionalIdResource;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.REGRESSION)
public class UsersST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(UsersST.class);
    private static final String VARIOUS_USER_ACLS_BUCKET = "VariousUserAcls";
    private static final String KAFKA_USER_1 = "testuser-1";
    private static final String KAFKA_USER_2 = "testuser-2";
    private static final String KAFKA_USER_3 = "testuser-3";
    private TestCaseConfig tcc;

    public Stream<Arguments> variousKafkaUsersAclsScenarios() {
        return Stream.of(
            Arguments.of(KAFKA_USER_1),
            Arguments.of(KAFKA_USER_2),
            Arguments.of(KAFKA_USER_3)
        );
    }

    /**
     * Tests that the KafkaUser detail page in the UI accurately reflects the ACLs configured on a
     * {@link KafkaUser} custom resource, for each of {@code testuser-1}, {@code testuser-2}, and {@code testuser-3}.
     *
     * <p>The test navigates directly to the single KafkaUser page via its URL and verifies that the
     * description list (name, username, authentication type, creation timestamp) matches the values
     * from the corresponding {@link KafkaUser} resource.</p>
     *
     * <p>It then compares the authorization table rendered in the UI against the {@code acls} defined
     * in the resource's {@link KafkaUserAuthorizationSimple} spec:</p>
     * <ul>
     *   <li>Asserts that the number of table rows matches the number of configured ACL rules.</li>
     *   <li>For each row, matches it to the corresponding ACL rule by resource type (cluster, group, topic,
     *       or transactional ID).</li>
     *   <li>Verifies resource-specific columns (name and pattern type) for group, topic, and transactional ID
     *       resources, and the placeholder {@code "-"} values for cluster-scoped resources.</li>
     *   <li>Verifies the common columns: resource type, host, comma-separated operations
     *       (e.g. {@code alter, create, write}), and ACL type (allow/deny).</li>
     * </ul>
     *
     * <p>This ensures that KafkaUser ACLs of different resource types are correctly surfaced and
     * formatted in the console UI.</p>
     */
    @TestBucket(VARIOUS_USER_ACLS_BUCKET)
    @ParameterizedTest(name = "{0}")
    @MethodSource("variousKafkaUsersAclsScenarios")
    void testVariousKafkaUsersAcls(String userName) {
        LOGGER.info("Verifying that the UI ACL table for KafkaUser '{}' matches its custom resource", userName);

        KafkaUser kafkaUser = ResourceUtils.getKubeResource(KafkaUser.class, tcc.namespace(), userName);
        String authenticationType = kafkaUser.getSpec().getAuthentication().getType();
        // Timestamp
        OffsetDateTime creationDateTimeOffset = Instant.parse(kafkaUser.getMetadata().getCreationTimestamp()).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        String creationTimestamp = creationDateTimeOffset.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
        LOGGER.debug("KafkaUser '{}' resource has authentication type '{}', created at '{}'", userName, authenticationType, creationTimestamp);

        // Verify navigation via url
        LOGGER.info("Navigating to single KafkaUser page for '{}'", userName);
        PwUtils.navigate(tcc, PwPageUrls.getSingleKafkaUserPage(tcc, tcc.kafkaName(), tcc.namespace(), userName));
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_KAFKA_USER_NAME_HEADER, userName, true);

        // Verify description list
        LOGGER.debug("Verifying description list (name, username, auth type, creation timestamp) for KafkaUser '{}'", userName);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_NAME, userName, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_USERNAME, userName, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_AUTH, authenticationType, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_CREATION_TIMESTAMP, creationTimestamp, true);

        List<AclRule> acls = ((KafkaUserAuthorizationSimple) kafkaUser.getSpec().getAuthorization()).getAcls();
        Locator rows = tcc.page().locator(KafkaUsersPageSelectors.KUPS_AUTHORIZATION_TABLE_ROWS);

        LOGGER.info("Verifying authorization table for KafkaUser '{}' contains {} ACL row(s)", userName, acls.size());
        assertEquals(acls.size(), rows.count(), "Mismatch between expected UI rows and actual KafkaUser ACL count");

        for (int i = 0; i < rows.count(); i++) {
            AclRule matchedAcl = null;

            Locator row = rows.nth(i);
            Locator cells = row.locator("td");
            String rowType = cells.nth(0).innerText().trim();
            LOGGER.debug("Matching UI ACL row {} of resource type '{}' to a KafkaUser ACL rule for '{}'", i, rowType, userName);

            for (AclRule acl : acls) {
                String aclType = acl.getResource().getType();
                if (rowType.equalsIgnoreCase(aclType)) {
                    matchedAcl = acl;
                    break;
                }
            }

            if (matchedAcl == null) {
                LOGGER.warn("No matching KafkaUser ACL rule found for UI resource type '{}' on user '{}'", rowType, userName);
                throw new AssertionError("No matching ACL found for UI " + rowType);
            }

            // Type specific checks
            switch (matchedAcl.getResource()) {
                case AclRuleTopicResource topic -> {
                    LOGGER.debug("Verifying topic ACL '{}' with pattern type '{}' for user '{}'", topic.getName(), topic.getPatternType(), userName);
                    PwUtils.waitForLocatorContainsText(cells.nth(1), topic.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), topic.getPatternType()
                        .toString(), false);
                }
                case AclRuleGroupResource group -> {
                    LOGGER.debug("Verifying group ACL '{}' with pattern type '{}' for user '{}'", group.getName(), group.getPatternType(), userName);
                    PwUtils.waitForLocatorContainsText(cells.nth(1), group.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), group.getPatternType()
                        .toString(), false);
                }
                case AclRuleTransactionalIdResource transactionalId -> {
                    LOGGER.debug("Verifying transactional ID ACL '{}' with pattern type '{}' for user '{}'", transactionalId.getName(), transactionalId.getPatternType(), userName);
                    PwUtils.waitForLocatorContainsText(cells.nth(1), transactionalId.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), transactionalId.getPatternType()
                        .toString(), false);
                }
                case AclRuleClusterResource clusterResource -> {
                    LOGGER.debug("Verifying cluster-scoped ACL (placeholder name/pattern type) for user '{}'", userName);
                    PwUtils.waitForLocatorContainsText(cells.nth(1), "-", false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), "-", false);
                }
                case  null, default -> {
                    LOGGER.warn("Encountered unsupported AclRule resource type for user '{}'", userName);
                    throw new AssertionError("Incorrect AclRule type");
                }
            }

            // Common columns
            String formattedOperations = KafkaUserTestUtils.getFormattedOperations(matchedAcl);
            LOGGER.debug("Verifying common ACL columns for user '{}': type '{}', host '{}', operations '{}', ACL type '{}'",
                userName, matchedAcl.getResource().getType(), matchedAcl.getHost(), formattedOperations, matchedAcl.getType());
            PwUtils.waitForLocatorContainsText(cells.nth(0), matchedAcl.getResource().getType(), false);
            PwUtils.waitForLocatorContainsText(cells.nth(3), matchedAcl.getHost(), false);
            PwUtils.waitForLocatorContainsText(cells.nth(4), formattedOperations, false);
            PwUtils.waitForLocatorContainsText(cells.nth(5), matchedAcl.getType().toString(), false);
        }
    }


    /**
     * Creates three {@link KafkaUser} resources, each using SCRAM-SHA-512 authentication and a distinct
     * combination of ACL rule types and operations, to back {@link #testVariousKafkaUsersAcls(String)}:
     * <ul>
     *   <li>{@code testuser-1}: a cluster resource ACL ({@code delete}, {@code describe},
     *       {@code idempotentwrite}), a literal group resource ACL named {@code *} ({@code all}), a
     *       literal topic resource ACL named {@code *} ({@code alterconfigs}, {@code clusteraction},
     *       {@code describeconfigs}), and a prefixed transactional ID resource ACL named
     *       {@code testAclName} ({@code alter}, {@code create}, {@code write}).</li>
     *   <li>{@code testuser-2}: a single literal transactional ID resource ACL named {@code testAclName}
     *       ({@code alter}, {@code delete}).</li>
     *   <li>{@code testuser-3}: a single prefixed group resource ACL named {@code testAclName}
     *       ({@code idempotentwrite}, {@code describeconfigs}).</li>
     * </ul>
     *
     * <p>After creation, the method waits for each user's corresponding Secret to become ready.</p>
     *
     * <p>This provides a variety of ACL resource types and operation combinations so the KafkaUser
     * detail page's authorization table rendering can be validated against real resource data.</p>
     */
    @SetupTestBucket(VARIOUS_USER_ACLS_BUCKET)
    public void setupVariousAcl() {
        LOGGER.info("Building KafkaUser '{}' with cluster, group, topic, and prefixed transactional ID ACLs", KAFKA_USER_1);
        KafkaUser user1 = new KafkaUserBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KAFKA_USER_1)
                .withNamespace(tcc.namespace())
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, tcc.kafkaName()))
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserScramSha512ClientAuthentication()
                .endKafkaUserScramSha512ClientAuthentication()
                .withNewKafkaUserAuthorizationSimple()
                    .addNewAcl()
                        .withNewAclRuleClusterResource()
                        .endAclRuleClusterResource()
                        .withOperations(AclOperation.DELETE)
                        .withOperations(AclOperation.DESCRIBE)
                        .withOperations(AclOperation.IDEMPOTENTWRITE)
                    .endAcl()
                    .addNewAcl()
                        .withNewAclRuleGroupResource()
                            .withName("*")
                            .withPatternType(AclResourcePatternType.LITERAL)
                        .endAclRuleGroupResource()
                        .withOperations(AclOperation.ALL)
                    .endAcl()
                    .addNewAcl()
                        .withNewAclRuleTopicResource()
                            .withName("*")
                            .withPatternType(AclResourcePatternType.LITERAL)
                        .endAclRuleTopicResource()
                        .withOperations(AclOperation.ALTERCONFIGS)
                        .withOperations(AclOperation.CLUSTERACTION)
                        .withOperations(AclOperation.DESCRIBECONFIGS)
                    .endAcl()
                    .addNewAcl()
                        .withNewAclRuleTransactionalIdResource()
                            .withName("testAclName")
                            .withPatternType(AclResourcePatternType.PREFIX)
                        .endAclRuleTransactionalIdResource()
                        .withOperations(AclOperation.ALTER)
                        .withOperations(AclOperation.CREATE)
                        .withOperations(AclOperation.WRITE)
                    .endAcl()
                .endKafkaUserAuthorizationSimple()
            .endSpec()
            .build();

        LOGGER.info("Building KafkaUser '{}' with a literal transactional ID ACL", KAFKA_USER_2);
        KafkaUser user2 = new KafkaUserBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KAFKA_USER_2)
                .withNamespace(tcc.namespace())
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, tcc.kafkaName()))
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserScramSha512ClientAuthentication()
                .endKafkaUserScramSha512ClientAuthentication()
                .withNewKafkaUserAuthorizationSimple()
                    .addNewAcl()
                        .withNewAclRuleTransactionalIdResource()
                            .withName("testAclName")
                            .withPatternType(AclResourcePatternType.LITERAL)
                        .endAclRuleTransactionalIdResource()
                        .withOperations(AclOperation.ALTER)
                        .withOperations(AclOperation.DELETE)
                    .endAcl()
                .endKafkaUserAuthorizationSimple()
            .endSpec()
            .build();

        LOGGER.info("Building KafkaUser '{}' with a prefixed group ACL", KAFKA_USER_3);
        KafkaUser user3 = new KafkaUserBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KAFKA_USER_3)
                .withNamespace(tcc.namespace())
                .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, tcc.kafkaName()))
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserScramSha512ClientAuthentication()
                .endKafkaUserScramSha512ClientAuthentication()
                .withNewKafkaUserAuthorizationSimple()
                    .addNewAcl()
                        .withNewAclRuleGroupResource()
                            .withName("testAclName")
                            .withPatternType(AclResourcePatternType.PREFIX)
                        .endAclRuleGroupResource()
                        .withOperations(AclOperation.IDEMPOTENTWRITE)
                        .withOperations(AclOperation.DESCRIBECONFIGS)
                    .endAcl()
                .endKafkaUserAuthorizationSimple()
            .endSpec()
            .build();

        LOGGER.info("Creating KafkaUsers '{}', '{}', '{}' in namespace '{}'", KAFKA_USER_1, KAFKA_USER_2, KAFKA_USER_3, tcc.namespace());
        KubeResourceManager.get().createResourceWithWait(user1, user2, user3);

        LOGGER.debug("Waiting for Secret of KafkaUser '{}' to become ready", user1.getMetadata().getName());
        WaitUtils.waitForSecretReady(user1.getMetadata().getNamespace(), user1.getMetadata().getName());
        LOGGER.debug("Waiting for Secret of KafkaUser '{}' to become ready", user2.getMetadata().getName());
        WaitUtils.waitForSecretReady(user2.getMetadata().getNamespace(), user2.getMetadata().getName());
        LOGGER.debug("Waiting for Secret of KafkaUser '{}' to become ready", user3.getMetadata().getName());
        WaitUtils.waitForSecretReady(user3.getMetadata().getNamespace(), user3.getMetadata().getName());
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

    @BeforeAll
    void testClassSetup() {
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
