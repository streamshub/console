package com.github.streamshub.systemtests.setup.keycloak;

import com.github.streamshub.console.api.v1alpha1.spec.security.Role;
import com.github.streamshub.console.api.v1alpha1.spec.security.RoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.console.api.v1alpha1.spec.security.Subject;
import com.github.streamshub.console.api.v1alpha1.spec.security.SubjectBuilder;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.systemtests.constants.AuthTestConstants;

import java.util.List;

public class KeycloakTestConfig {

    // --------------------------------
    // ----------- Keycloak -----------
    // --------------------------------
    public record User(
        String username,
        String password,
        String groupPath,
        String firstName,
        String lastName,
        String email
    ) { }

    public record GroupRoleMapping(
        // Keycloak group - "developers-team"
        String groupName,
        // Group path - "/developers-team"
        String groupPath,
        // Keycloak role - "developers-role"
        String roleName,
        // Just a simple description
        String roleDescription,
        // Console role - "dev"
        String consoleRoleName,
        // which kafkas this role can access
        List<String> kafkaNames,
        List<Rule.Privilege> privileges
    ) { }

    public static final List<User> DEFAULT_USER_MAPPING = List.of(
        new User(AuthTestConstants.USER_DEV_BOB, AuthTestConstants.USER_DEV_BOB, AuthTestConstants.DEV_GROUP_PATH,
            AuthTestConstants.USER_DEV_BOB, AuthTestConstants.USER_DEV_BOB_LASTNAME, AuthTestConstants.USER_DEV_BOB_EMAIL),
        new User(AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.ADMIN_GROUP_PATH,
            AuthTestConstants.USER_ADMIN_ALICE, AuthTestConstants.USER_ADMIN_ALICE_LASTNAME, AuthTestConstants.USER_ADMIN_ALICE_EMAIL),
        new User(AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.TOPICS_VIEW_GROUP_PATH,
            AuthTestConstants.USER_TOPICONLY_FRANK, AuthTestConstants.USER_TOPICONLY_FRANK_LASTNAME, AuthTestConstants.USER_TOPICONLY_FRANK_EMAIL),
        new User(AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.CONSUMERGROUPS_VIEW_GROUP_PATH,
            AuthTestConstants.USER_CONSUMERONLY_GRACE, AuthTestConstants.USER_CONSUMERONLY_GRACE_LASTNAME, AuthTestConstants.USER_CONSUMERONLY_GRACE_EMAIL)
    );

    public static final List<GroupRoleMapping> DEFAULT_ROLE_MAPPING = List.of(
        new GroupRoleMapping(
            AuthTestConstants.DEV_GROUP_NAME, AuthTestConstants.DEV_GROUP_PATH,
            AuthTestConstants.DEV_ROLE_NAME, AuthTestConstants.DEV_ROLE_DESCRIPTION,
            AuthTestConstants.DEV_ROLE_NAME,
            List.of(AuthTestConstants.TEAM_DEV_KAFKA_NAME),
            List.of(Rule.Privilege.ALL)),
        new GroupRoleMapping(
            AuthTestConstants.ADMIN_GROUP_NAME, AuthTestConstants.ADMIN_GROUP_PATH,
            AuthTestConstants.ADMIN_ROLE_NAME, AuthTestConstants.ADMIN_ROLE_DESCRIPTION,
            AuthTestConstants.ADMIN_ROLE_NAME,
            List.of(AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_DEV_KAFKA_NAME),
            List.of(Rule.Privilege.ALL)),
        new GroupRoleMapping(
            AuthTestConstants.TOPICS_VIEW_GROUP_NAME, AuthTestConstants.TOPICS_VIEW_GROUP_PATH,
            AuthTestConstants.TOPICS_VIEW_ROLE_NAME, AuthTestConstants.TOPICS_VIEW_ROLE_DESCRIPTION,
            AuthTestConstants.TOPICS_VIEW_ROLE_NAME,
            List.of(AuthTestConstants.TEAM_DEV_KAFKA_NAME),
            List.of(Rule.Privilege.GET, Rule.Privilege.LIST)),
        new GroupRoleMapping(
            AuthTestConstants.CONSUMERGROUPS_VIEW_GROUP_NAME, AuthTestConstants.CONSUMERGROUPS_VIEW_GROUP_PATH,
            AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME, AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_DESCRIPTION,
            AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME,
            List.of(AuthTestConstants.TEAM_DEV_KAFKA_NAME),
            List.of(Rule.Privilege.GET, Rule.Privilege.LIST))
    );

    public static List<Subject> getSubjects(List<GroupRoleMapping> mapping) {
        return mapping.stream()
            .map(m ->
                new SubjectBuilder()
                    .withClaim(AuthTestConstants.CLAIM_GROUPS)
                    .addToInclude(m.groupPath())
                    .addToRoleNames(m.consoleRoleName())
                    .build())
            .toList();
    }

    public static List<Role> getRoles(List<GroupRoleMapping> mapping) {
        return mapping.stream()
            .map(m -> new RoleBuilder()
                .withName(m.consoleRoleName())
                .addNewRule()
                    .withResources(ResourceTypes.Global.KAFKAS.value())
                    .withResourceNames(m.kafkaNames().toArray(new String[0]))
                    .withPrivileges(m.privileges().toArray(new Rule.Privilege[0]))
                .endRule()
                .build())
            .toList();
    }

    // --------------------------------
    // ------------- Kafka ------------
    // --------------------------------

    public record KafkaRoleMapping(
        String roleName,
        List<String> resources,
        List<Rule.Privilege> privileges
    ) { }

    public record KafkaConfig(
        String kafkaName,
        List<KafkaRoleMapping> roles
    ) { }

    public static final List<KafkaConfig> DEFAULT_KAFKA_CLUSTERS_MAPPING = List.of(
        new KafkaConfig(
            AuthTestConstants.TEAM_DEV_KAFKA_NAME,
            List.of(
                new KafkaRoleMapping(
                    AuthTestConstants.DEV_ROLE_NAME,
                    ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList(),
                    List.of(Rule.Privilege.GET, Rule.Privilege.LIST)),
                new KafkaRoleMapping(
                    AuthTestConstants.ADMIN_ROLE_NAME,
                    ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList(),
                    List.of(Rule.Privilege.ALL)),
                new KafkaRoleMapping(
                    AuthTestConstants.TOPICS_VIEW_ROLE_NAME,
                    List.of(ResourceTypes.Kafka.TOPICS.value(), ResourceTypes.Kafka.TOPIC_RECORDS.value()),
                    List.of(Rule.Privilege.GET, Rule.Privilege.LIST)),
                new KafkaRoleMapping(
                    AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME,
                    List.of(ResourceTypes.Kafka.GROUPS.value()),
                    List.of(Rule.Privilege.GET, Rule.Privilege.LIST))
            )),
        new KafkaConfig(
            AuthTestConstants.TEAM_ADMIN_KAFKA_NAME,
            List.of(
                new KafkaRoleMapping(
                    AuthTestConstants.ADMIN_ROLE_NAME,
                    ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList(),
                    List.of(Rule.Privilege.ALL))
            ))
    );
}
