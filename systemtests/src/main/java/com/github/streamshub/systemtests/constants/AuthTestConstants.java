package com.github.streamshub.systemtests.constants;

public class AuthTestConstants {
    public static final int DEV_REPLICATED_TOPICS_COUNT = 3;
    public static final int ADMIN_REPLICATED_TOPICS_COUNT = 5;

    private AuthTestConstants() {}

    // JWT Claim
    public static final String CLAIM_GROUPS = "groups";

    // Test Usernames
    public static final String USER_DEV_BOB = "bob";
    public static final String USER_DEV_BOB_LASTNAME = "TheDeveloper";

    public static final String USER_ADMIN_ALICE = "alice";
    public static final String USER_ADMIN_ALICE_LASTNAME = "TheAdmin";

    public static final String USER_TOPICONLY_FRANK = "frank";
    public static final String USER_TOPICONLY_FRANK_LASTNAME = "TopicOnlyViewer";

    public static final String USER_CONSUMERONLY_GRACE = "grace";
    public static final String USER_CONSUMERONLY_GRACE_LASTNAME = "ConsuemerOnlyViewer";

    // Suffixes
    public static final String GROUP_SUFFIX = "-team";
    public static final String ROLE_SUFFIX = "-role";

    // Base names
    public static final String DEV = "developers";
    public static final String ADMIN = "administrators";
    public static final String TOPICS_VIEW = "topics";
    public static final String CONSUMERGROUPS_VIEW = "consumergroups";

    // Keycloak Groups
    public static final String DEV_GROUP_NAME = DEV + GROUP_SUFFIX;
    public static final String ADMIN_GROUP_NAME = ADMIN + GROUP_SUFFIX;
    public static final String TOPICS_VIEW_GROUP_NAME = TOPICS_VIEW + "-viewonly" + GROUP_SUFFIX;
    public static final String CONSUMERGROUPS_VIEW_GROUP_NAME = CONSUMERGROUPS_VIEW + "-viewonly" + GROUP_SUFFIX;

    // Keycloak Role Names
    public static final String DEV_ROLE_NAME = DEV + ROLE_SUFFIX;
    public static final String ADMIN_ROLE_NAME = ADMIN + ROLE_SUFFIX;
    public static final String TOPICS_VIEW_ROLE_NAME = TOPICS_VIEW + ROLE_SUFFIX;
    public static final String CONSUMERGROUPS_VIEW_ROLE_NAME = CONSUMERGROUPS_VIEW + ROLE_SUFFIX;

    // Keycloak Group Paths
    public static final String DEV_GROUP_PATH = "/" + DEV_GROUP_NAME; // NOSONAR
    public static final String ADMIN_GROUP_PATH = "/" + ADMIN_GROUP_NAME; // NOSONAR
    public static final String TOPICS_VIEW_GROUP_PATH = "/" + TOPICS_VIEW_GROUP_NAME; // NOSONAR
    public static final String CONSUMERGROUPS_VIEW_GROUP_PATH = "/" + CONSUMERGROUPS_VIEW_GROUP_NAME; // NOSONAR

    // Keycloak Role Descriptions
    public static final String DEV_ROLE_DESCRIPTION = DEV_ROLE_NAME + " member";
    public static final String ADMIN_ROLE_DESCRIPTION = ADMIN_ROLE_NAME + " member";
    public static final String TOPICS_VIEW_ROLE_DESCRIPTION = "Can view " + TOPICS_VIEW_ROLE_NAME + "s only";
    public static final String CONSUMERGROUPS_VIEW_ROLE_DESCRIPTION = "Can view " + CONSUMERGROUPS_VIEW_ROLE_NAME + "s only";

    // Aditional emails
    public static final String USER_DEV_BOB_EMAIL = AuthTestConstants.USER_DEV_BOB + "@streams-hub.io";
    public static final String USER_ADMIN_ALICE_EMAIL = AuthTestConstants.USER_ADMIN_ALICE + "@streams.hub.io";
    public static final String USER_TOPICONLY_FRANK_EMAIL = AuthTestConstants.USER_TOPICONLY_FRANK + "@stream+shub.io";
    public static final String USER_CONSUMERONLY_GRACE_EMAIL = AuthTestConstants.USER_CONSUMERONLY_GRACE + "@streamshub.io";

    // Kafka Cluster Names
    public static final String TEAM_DEV_KAFKA_NAME = "kc-team-dev";
    public static final String TEAM_DEV_TOPIC_PREFIX = "dev-";
    public static final String TEAM_ADMIN_KAFKA_NAME = "kc-team-admin";
    public static final String TEAM_ADMIN_TOPIC_PREFIX = "admin-";

}
