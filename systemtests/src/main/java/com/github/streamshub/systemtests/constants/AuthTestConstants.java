package com.github.streamshub.systemtests.constants;

public class AuthTestConstants {
    public static final int DEV_REPLICATED_TOPICS_COUNT = 3;
    public static final int ADMIN_REPLICATED_TOPICS_COUNT = 5;

    private AuthTestConstants() {}

    // Keycloak Groups
    public static final String DEV_GROUP_NAME = "/developers-team";
    public static final String ADMIN_GROUP_NAME = "/administrators-team";
    public static final String TOPICS_VIEW_GROUP_NAME = "/topics-view-team";
    public static final String CONSUMERGROUPS_VIEW_GROUP_NAME = "/consumergroups-view-team";

    // Console Roles
    public static final String DEV_ROLE_NAME = "developers-role";
    public static final String ADMIN_ROLE_NAME = "administrators-role";
    public static final String TOPICS_VIEW_ROLE_NAME = "topics-role";
    public static final String CONSUMERGROUPS_VIEW_ROLE_NAME = "consumergroups-role";

    // Kafka Cluster Names
    public static final String TEAM_DEV_KAFKA_NAME = "kc-team-dev";
    public static final String TEAM_DEV_TOPIC_PREFIX = "dev-";
    public static final String TEAM_ADMIN_KAFKA_NAME = "kc-team-admin";
    public static final String TEAM_ADMIN_TOPIC_PREFIX = "admin-";

    // JWT Claim
    public static final String CLAIM_GROUPS = "groups";

    // Test Usernames
    // developer
    public static final String USER_DEV_BOB = "bob";
    // admin
    public static final String USER_ADMIN_ALICE = "alice";
    // topics only
    public static final String USER_TOPICONLY_FRANK = "frank";
    // consumer groups only
    public static final String USER_CONSUMERONLY_GRACE = "grace";
    // both topics and consumer view
    public static final String USER_CROSS_CAROL = "carol";
    // no access
    public static final String USER_NOACCESS_HANK = "hank";
}
