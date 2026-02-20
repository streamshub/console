package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.KafkaUsersPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testutils.KafkaUserTestUtils;
import com.microsoft.playwright.Locator;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthorizationSimple;
import io.strimzi.api.kafka.model.user.acl.AclRule;
import io.strimzi.api.kafka.model.user.acl.AclRuleClusterResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleGroupResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTopicResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTransactionalIdResource;

import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsersChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(UsersChecks.class);

    public static void checkAclTableWithMatchingKafkaUser(TestCaseConfig tcc, String userName) {
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
}
