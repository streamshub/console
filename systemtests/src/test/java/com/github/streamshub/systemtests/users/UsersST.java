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
import io.skodjob.testframe.resources.KubeResourceManager;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @TestBucket(VARIOUS_USER_ACLS_BUCKET)
    @ParameterizedTest(name = "{0}")
    @MethodSource("variousKafkaUsersAclsScenarios")
    void testVariousKafkaUsersAcls(String userName) {
        LOGGER.info("Verify UI ACLs of kafka user {}", userName);

        KafkaUser kafkaUser = ResourceUtils.getKubeResource(KafkaUser.class, tcc.namespace(), userName);
        String authenticationType = kafkaUser.getSpec().getAuthentication().getType();
        // Timestamp
        OffsetDateTime creationDateTimeOffset = Instant.parse(kafkaUser.getMetadata().getCreationTimestamp()).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        String creationTimestamp = creationDateTimeOffset.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));

        // Verify navigation via url
        tcc.page().navigate(PwPageUrls.getSingleKafkaUserPage(tcc, tcc.kafkaName(), tcc.namespace(), userName));
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_KAFKA_USER_NAME_HEADER, userName, true);

        // Verify description list
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_NAME, userName, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_USERNAME, userName, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_AUTH, authenticationType, true);
        PwUtils.waitForContainsText(tcc, KafkaUsersPageSelectors.KUPS_DESCRIPTION_CREATION_TIMESTAMP, creationTimestamp, true);

        List<AclRule> acls = ((KafkaUserAuthorizationSimple) kafkaUser.getSpec().getAuthorization()).getAcls();
        Locator rows = tcc.page().locator(KafkaUsersPageSelectors.KUPS_AUTHORIZATION_TABLE_ROWS);

        assertEquals(acls.size(), rows.count(), "Mismatch between expected UI rows and actual KafkaUser ACL count");

        for (int i = 0; i < rows.count(); i++) {
            AclRule matchedAcl = null;

            Locator row = rows.nth(i);
            Locator cells = row.locator("td");
            String rowType = cells.nth(0).innerText().trim();

            for (AclRule acl : acls) {
                String aclType = acl.getResource().getType();
                if (rowType.equalsIgnoreCase(aclType)) {
                    matchedAcl = acl;
                    break;
                }
            }

            if (matchedAcl == null) {
                throw new AssertionError("No matching ACL found for UI " + rowType);
            }

            // Type specific checks
            switch (matchedAcl.getResource()) {
                case AclRuleTopicResource topic -> {
                    PwUtils.waitForLocatorContainsText(cells.nth(1), topic.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), topic.getPatternType()
                        .toString(), false);
                }
                case AclRuleGroupResource group -> {
                    PwUtils.waitForLocatorContainsText(cells.nth(1), group.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), group.getPatternType()
                        .toString(), false);
                }
                case AclRuleTransactionalIdResource transactionalId -> {
                    PwUtils.waitForLocatorContainsText(cells.nth(1), transactionalId.getName(), false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), transactionalId.getPatternType()
                        .toString(), false);
                }
                case AclRuleClusterResource clusterResource -> {
                    PwUtils.waitForLocatorContainsText(cells.nth(1), "-", false);
                    PwUtils.waitForLocatorContainsText(cells.nth(2), "-", false);
                }
                case  null, default -> {
                    throw new AssertionError("Incorrect AclRule type");
                }
            }

            // Common columns
            String formattedOperations = KafkaUserTestUtils.getFormattedOperations(matchedAcl);
            PwUtils.waitForLocatorContainsText(cells.nth(0), matchedAcl.getResource().getType(), false);
            PwUtils.waitForLocatorContainsText(cells.nth(3), matchedAcl.getHost(), false);
            PwUtils.waitForLocatorContainsText(cells.nth(4), formattedOperations, false);
            PwUtils.waitForLocatorContainsText(cells.nth(5), matchedAcl.getType().toString(), false);
        }
    }


    @SetupTestBucket(VARIOUS_USER_ACLS_BUCKET)
    public void setupVariousAcl() {
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

        KubeResourceManager.get().createResourceWithWait(user1, user2, user3);
        WaitUtils.waitForSecretReady(user1.getMetadata().getNamespace(), user1.getMetadata().getName());
        WaitUtils.waitForSecretReady(user2.getMetadata().getNamespace(), user2.getMetadata().getName());
        WaitUtils.waitForSecretReady(user3.getMetadata().getNamespace(), user3.getMetadata().getName());
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
