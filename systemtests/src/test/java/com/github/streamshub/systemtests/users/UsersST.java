package com.github.streamshub.systemtests.users;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.testchecks.UsersChecks;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;

@Tag(TestTags.REGRESSION)
public class UsersST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(UsersST.class);
    private static final String VARIOUS_USER_ACLS_BUCKET = "VariousUserAcls";
    private static final String KAFKA_USER_1 = "testUser-1";
    private static final String KAFKA_USER_2 = "testUser-2";
    private static final String KAFKA_USER_3 = "testUser-3";
    private TestCaseConfig tcc;

    @Test
    @TestBucket(VARIOUS_USER_ACLS_BUCKET)
    void testVariousKafkaUsers() {
        TestCaseConfig tcc = getTestCaseConfig();
        UsersChecks.checkAclTableWithMatchingKafkaUser(tcc, KAFKA_USER_1);
        UsersChecks.checkAclTableWithMatchingKafkaUser(tcc, KAFKA_USER_2);
        UsersChecks.checkAclTableWithMatchingKafkaUser(tcc, KAFKA_USER_3);
    }


    @SetupTestBucket(VARIOUS_USER_ACLS_BUCKET)
    public void setupVariousAcl() {
        KafkaUser user1 = new KafkaUserBuilder()
            .withApiVersion(Constants.STRIMZI_API_V1)
            .withNewMetadata()
                .withName(KafkaNamingUtils.kafkaUserName(KAFKA_USER_1))
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
                .withName(KafkaNamingUtils.kafkaUserName(KAFKA_USER_2))
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
                .withName(KafkaNamingUtils.kafkaUserName(KAFKA_USER_3))
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
