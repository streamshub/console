package org.bf2.admin.kafka.systemtest.oauth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.Method;
import io.restassured.response.ValidatableResponse;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.bf2.admin.kafka.admin.AccessControlOperations;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;
import org.bf2.admin.kafka.systemtest.TestOAuthProfile;
import org.bf2.admin.kafka.systemtest.deployment.DeploymentManager.UserType;
import org.bf2.admin.kafka.systemtest.utils.ClientsConfig;
import org.bf2.admin.kafka.systemtest.utils.TokenUtils;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue.ValueType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import static io.restassured.RestAssured.given;
import static org.bf2.admin.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@TestProfile(TestOAuthProfile.class)
class AccessControlListIT {

    static final String SORT_ASC = "asc";
    static final String SORT_DESC = "desc";

    @Inject
    Config config;

    String validResourceOperations;
    TokenUtils tokenUtils;

    @BeforeEach
    void setup() {
        validResourceOperations = config.getValue(KafkaAdminConfigRetriever.ACL_RESOURCE_OPERATIONS, String.class);
        tokenUtils = new TokenUtils(config);
    }

    @AfterEach
    void cleanup() {
        deleteAcls(UserType.OWNER, Map.of());
    }

    @ParameterizedTest
    @CsvSource({
        "OWNER,   OK",
        "USER,    FORBIDDEN",
        "OTHER,   FORBIDDEN",
        "INVALID, UNAUTHORIZED"
    })
    void testGetAclsByUserType(UserType userType, Status expectedStatus) {
        getAcls(userType, Map.of(), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
        "OWNER,   OK          ,               , OK          , ",
        "USER,    FORBIDDEN   , NOT_AUTHORIZED, OK          , ",
        "OTHER,   FORBIDDEN   , NOT_AUTHORIZED, FORBIDDEN   , NOT_AUTHORIZED", // OTHER user always restricted from cluster (per Keycloak RBAC)
        "INVALID, UNAUTHORIZED,               , UNAUTHORIZED, "
    })
    void testGetAclsByUserTypeWithGrant(UserType userType, Status beforeStatus, ErrorType beforeError, Status afterStatus, ErrorType afterError) {
        JsonObject newBinding = aclBinding(ResourceType.CLUSTER,
                                    "kafka-cluster",
                                    PatternType.LITERAL,
                                    "User:" + UserType.USER.getUsername(),
                                    AclOperation.DESCRIBE,
                                    AclPermissionType.ALLOW);

        var beforeResponse = getAcls(userType, Map.of(), beforeStatus);

        if (beforeStatus == Status.OK) {
            beforeResponse.body("items.size()", equalTo(0));
        } else if (userType == UserType.INVALID) {
            /*
             * Quarkus forces an empty 401 response when the token is not valid
             * https://github.com/quarkusio/quarkus/issues/22971
             */
            beforeResponse.header("content-length", equalTo("0"));
        } else {
            beforeResponse.body("", matchesError(beforeError));
        }

        createAcl(UserType.OWNER, newBinding.toString()); // Grant allow describe on cluster to USER

        var afterResponse = getAcls(userType, Map.of(), afterStatus);

        if (afterStatus == Status.OK) {
            afterResponse.body("items", hasSize(1))
                .body("items.find { it }.resourceName", equalTo("kafka-cluster"));
        } else if (userType == UserType.INVALID) {
            /*
             * Quarkus forces an empty 401 response when the token is not valid
             * https://github.com/quarkusio/quarkus/issues/22971
             */
            afterResponse.header("content-length", equalTo("0"));
        } else {
            beforeResponse.body("", matchesError(afterError));
        }
    }

    @Test
    void testCreateAclsDeniedInvalid() {
        JsonObject newBinding = aclBinding(ResourceType.CLUSTER,
                                           "kafka-cluster",
                                           PatternType.LITERAL,
                                           "User:" + UserType.USER.getUsername(),
                                           AclOperation.ALL,
                                           AclPermissionType.ALLOW);

        final ErrorType expectedError = ErrorType.INVALID_ACL_RESOURCE_OP;

        createAcl(UserType.OWNER, newBinding.toString(), expectedError.getHttpStatus())
            .body("", matchesError(expectedError));
    }

    @Test
    void testGetAclsByUserIncludesWildcard() {
        String principal = "User:" + UserType.USER.getUsername();
        JsonObject binding1 = aclBinding(ResourceType.TOPIC, "user_topic", PatternType.LITERAL, principal, AclOperation.READ, AclPermissionType.ALLOW);
        JsonObject binding2 = aclBinding(ResourceType.TOPIC, "public_topic", PatternType.LITERAL, "User:*", AclOperation.READ, AclPermissionType.ALLOW);
        List<JsonObject> newBindings = List.of(binding1, binding2);

        createAcls(UserType.OWNER, newBindings);

        var response = getAcls(UserType.OWNER, Map.of("principal", principal));
        response.body("items", hasSize(2));

        var bindings = Json.createReader(response.extract().asInputStream()).readObject().getJsonArray("items");

        List<JsonObject> createdBindings = bindings.stream().map(JsonObject.class::cast).collect(Collectors.toList());
        assertTrue(newBindings.stream().allMatch(createdBindings::contains), () ->
            "Response " + bindings + " did not contain one of " + newBindings);
    }

    @Test
    void testGetAclsByWildcardExcludesUser() {
        String principal = "User:" + UserType.USER.getUsername();
        JsonObject binding1 = aclBinding(ResourceType.TOPIC, "user_topic", PatternType.LITERAL, principal, AclOperation.READ, AclPermissionType.ALLOW);
        JsonObject binding2 = aclBinding(ResourceType.TOPIC, "public_topic", PatternType.LITERAL, "User:*", AclOperation.READ, AclPermissionType.ALLOW);
        List<JsonObject> newBindings = List.of(binding1, binding2);

        createAcls(UserType.OWNER, newBindings);

        var response = getAcls(UserType.OWNER, Map.of("principal", "User:*"));
        response.body("items", hasSize(1));

        List<JsonObject> createdBindings = Json.createReader(response.extract().asInputStream())
                .readObject()
                .getJsonArray("items")
                .stream()
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());

        assertTrue(createdBindings.stream().anyMatch(binding2::equals), () ->
            "Response " + createdBindings + " did not contain " + binding2);
    }

    @Test
    void testGetAclsSortsDenyFirst() {
        String principal = "User:" + UserType.USER.getUsername();

        List<JsonObject> newBindings = IntStream.range(0, 20)
                .mapToObj(index -> aclBinding(ResourceType.TOPIC, "topic" + index,
                                              PatternType.LITERAL,
                                              principal,
                                              AclOperation.READ,
                                              index % 2 == 0 ? AclPermissionType.ALLOW : AclPermissionType.DENY))
                .collect(Collectors.toList());

        createAcls(UserType.OWNER, newBindings);

        var response = getAcls(UserType.OWNER, Map.of("page", "1", "size", "10"));

        response
            .body("page", equalTo(1))
            .body("size", equalTo(10))
            .body("total", equalTo(newBindings.size()))
            .body("items", hasSize(10));

        List<JsonObject> createdBindings = Json.createReader(response.extract().asInputStream())
                .readObject()
                .getJsonArray("items")
                .stream()
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());

        assertTrue(createdBindings.stream()
               .map(JsonObject.class::cast)
               .map(b -> b.getString("permission"))
               .map(AclPermissionType::valueOf)
               .allMatch(AclPermissionType.DENY::equals), () -> "Response " + createdBindings + " were not all DENY");

//            .compose(ignored -> )
//            .compose(HttpClientResponse::body)
//            .map(buffer -> new JsonObject(buffer))
//            .map(response -> testContext.verify(() -> {
//                assertEquals(20, response.getInteger("total"));
//                assertEquals(1, response.getInteger("page"));
//                assertEquals(10, response.getInteger("size"));
//
//                JsonArray bindings = response.getJsonArray("items");
//                assertEquals(10, bindings.size());
//                assertTrue(bindings.stream()
//                           .map(JsonObject.class::cast)
//                           .map(b -> b.getString("permission"))
//                           .map(AclPermissionType::valueOf)
//                           .allMatch(AclPermissionType.DENY::equals), () ->
//                    "Response " + bindings + " were not all DENY");
//                responseBodyVerified.flag();
//            }))
//            .onFailure(testContext::failNow);
    }

    @ParameterizedTest
    @CsvSource({
        Types.AclBinding.PROP_PERMISSION + "," + SORT_ASC,
        Types.AclBinding.PROP_PERMISSION + "," + SORT_DESC,
        Types.AclBinding.PROP_RESOURCE_TYPE + "," + SORT_ASC,
        Types.AclBinding.PROP_RESOURCE_TYPE + "," + SORT_DESC,
        Types.AclBinding.PROP_PATTERN_TYPE + "," + SORT_ASC,
        Types.AclBinding.PROP_PATTERN_TYPE + "," + SORT_DESC,
        Types.AclBinding.PROP_OPERATION + "," + SORT_ASC,
        Types.AclBinding.PROP_OPERATION + "," + SORT_DESC,
        Types.AclBinding.PROP_PRINCIPAL + "," + SORT_ASC,
        Types.AclBinding.PROP_PRINCIPAL + "," + SORT_DESC,
        Types.AclBinding.PROP_RESOURCE_NAME + "," + SORT_ASC,
        Types.AclBinding.PROP_RESOURCE_NAME + "," + SORT_DESC,
    })
    void testGetAclsOrderByProperies(String orderKey, String order) throws Exception {
        JsonObject allowedResourceOperations = Json.createReader(new StringReader(validResourceOperations)).readObject();

        List<JsonObject> newBindings = Stream.of(Json.createObjectBuilder().build())
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_PERMISSION, AclPermissionType.ALLOW, AclPermissionType.DENY))
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_RESOURCE_TYPE, ResourceType.TOPIC, ResourceType.GROUP, ResourceType.CLUSTER, ResourceType.TRANSACTIONAL_ID))
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_PATTERN_TYPE, PatternType.LITERAL, PatternType.PREFIXED))
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_OPERATION, AclOperation.READ,
                                     AclOperation.ALL, AclOperation.ALTER, AclOperation.DELETE,
                                     AclOperation.CREATE, AclOperation.ALTER_CONFIGS,
                                     AclOperation.DESCRIBE, AclOperation.DESCRIBE_CONFIGS, AclOperation.WRITE))
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_PRINCIPAL, "User:{uuid}"))
            .flatMap(binding -> join(binding, Types.AclBinding.PROP_RESOURCE_NAME, "resource-{uuid}"))
            .filter(binding -> {
                String resourceType = binding.getString(Types.AclBinding.PROP_RESOURCE_TYPE).toLowerCase(Locale.US);
                String operation = binding.getString(Types.AclBinding.PROP_OPERATION).toLowerCase(Locale.US);

                return allowedResourceOperations.getJsonArray(resourceType)
                    .stream()
                    .filter(value -> value.getValueType() == ValueType.STRING)
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .anyMatch(operation::equals);
            })
            .map(binding -> {
                if (ResourceType.CLUSTER.name().equals(binding.getString(Types.AclBinding.PROP_RESOURCE_TYPE))) {
                    // Only value allowed is "kafka-cluster"
                    binding = Json.createObjectBuilder(binding)
                            .add(Types.AclBinding.PROP_RESOURCE_NAME, "kafka-cluster")
                            .build();
                }
                return binding;
            })
            .distinct()
            .collect(Collectors.toList());

        List<String> sortKeys = new LinkedList<>(AccessControlOperations.SORT_KEYS.keySet());
        // Remove the primary sort key, handled as a special case
        sortKeys.remove(orderKey);

        List<JsonObject> expectedValues = newBindings.stream()
            .map(JsonObject.class::cast)
            .sorted((j1, j2) -> {
                int result;

                if ((result = j1.getString(orderKey).compareTo(j2.getString(orderKey))) != 0) {
                    return SORT_DESC.equals(order) ? (result * -1) : result;
                }

                for (String key : sortKeys) {
                    if ((result = j1.getString(key).compareTo(j2.getString(key))) != 0) {
                        return result;
                    }
                }

                return 0;
            })
            .collect(Collectors.toList());

        final int expectedTotal = newBindings.size();
        final int pageSize = expectedTotal + 1;
        final var queryParams = Map.of("page", "1", "size", String.valueOf(pageSize), "orderKey", orderKey, "order", order);

        Properties adminConfig = ClientsConfig.getAdminConfigOauth(config, tokenUtils.getToken(UserType.OWNER.getUsername()));

        /*
         * Due to the number of ACLs created for this case (> 200), using the
         * bulk API directly is necessary.
         */
        try (Admin admin = Admin.create(adminConfig)) {
            admin.createAcls(newBindings.stream()
                               .map(Types.AclBinding::fromJsonObject)
                               .map(Types.AclBinding::toKafkaBinding)
                               .collect(Collectors.toList()))
                .all()
                .whenComplete((result, error) -> {
                    if (error != null) {
                        fail(error);
                    } else {
                        var response = getAcls(UserType.OWNER, queryParams)
                            .body("total", equalTo(expectedTotal))
                            .body("size", equalTo(pageSize))
                            .body("page", equalTo(1))
                            .body("items", hasSize(expectedTotal));

                        JsonObject responseBody = Json.createReader(response.extract().asInputStream()).readObject();
                        List<JsonObject> responseValues = responseBody.getJsonArray("items")
                            .stream()
                            .map(JsonObject.class::cast)
                            .map(obj ->
                                Json.createObjectBuilder()
                                    .add(Types.AclBinding.PROP_PERMISSION, obj.get(Types.AclBinding.PROP_PERMISSION))
                                    .add(Types.AclBinding.PROP_RESOURCE_TYPE, obj.get(Types.AclBinding.PROP_RESOURCE_TYPE))
                                    .add(Types.AclBinding.PROP_PATTERN_TYPE, obj.get(Types.AclBinding.PROP_PATTERN_TYPE))
                                    .add(Types.AclBinding.PROP_OPERATION, obj.get(Types.AclBinding.PROP_OPERATION))
                                    .add(Types.AclBinding.PROP_PRINCIPAL, obj.get(Types.AclBinding.PROP_PRINCIPAL))
                                    .add(Types.AclBinding.PROP_RESOURCE_NAME, obj.get(Types.AclBinding.PROP_RESOURCE_NAME))
                                    .build())
                            .collect(Collectors.toList());

                        assertEquals(expectedValues, responseValues, "Unexpected response order");
                    }
                })
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS);
        }
    }

    // Utilities

    ValidatableResponse getAcls(UserType userType, Map<String, String> filters, StatusType expectedStatus) {
        return aclRequest(Method.GET, filters, userType, null, expectedStatus);
    }

    ValidatableResponse getAcls(UserType userType, Map<String, String> filters) {
        return getAcls(userType, filters, Status.OK);
    }

    ValidatableResponse createAcl(UserType userType, String newBinding, StatusType expectedStatus) {
        return aclRequest(Method.POST, Map.of(), userType, newBinding, expectedStatus);
    }

    ValidatableResponse createAcl(UserType userType, String newBinding) {
        return createAcl(userType, newBinding, Status.CREATED);
    }

    void createAcls(UserType userType, List<JsonObject> newBindings) {
        newBindings.stream().forEach(binding -> createAcl(userType, binding.toString()));
    }

    ValidatableResponse deleteAcls(UserType userType, Map<String, String> filters) {
        return aclRequest(Method.DELETE, filters, userType, null, Status.OK);
    }

    ValidatableResponse aclRequest(Method method,
                                  Map<String, String> filters,
                                  UserType userType,
                                  String body,
                                  StatusType expectedStatus) {

        var request = given()
                .header(tokenUtils.authorizationHeader(userType.getUsername()));

        if (body != null) {
            request = request
                    .header("content-type", "application/json")
                    .body(body);
        }

        return request
            .log().ifValidationFails()
        .when()
            .queryParams(filters)
            .request(Method.valueOf(method.name()), "/rest/acls")
        .then()
            .log().ifValidationFails()
            .statusCode(expectedStatus.getStatusCode());
    }

    JsonObject aclBinding(ResourceType resourceType,
                          String resourceName,
                          PatternType patternType,
                          String principal,
                          AclOperation operation,
                          AclPermissionType permission) {
        return Json.createObjectBuilder()
                .add(Types.AclBinding.PROP_RESOURCE_TYPE, resourceType.name())
                .add(Types.AclBinding.PROP_RESOURCE_NAME, resourceName)
                .add(Types.AclBinding.PROP_PATTERN_TYPE, patternType.name())
                .add(Types.AclBinding.PROP_PRINCIPAL, principal)
                .add(Types.AclBinding.PROP_OPERATION, operation.name())
                .add(Types.AclBinding.PROP_PERMISSION, permission.name())
                .add("kind", "AclBinding")
                .build();
    }

    Stream<JsonObject> join(JsonObject base, String newKey, Object... values) {
        List<JsonObject> results = new ArrayList<>(values.length);

        for (Object value : values) {
            JsonObjectBuilder copy = Json.createObjectBuilder(base);
            String newValue = value.toString();

            if (newValue != null && newValue.contains("{uuid}")) {
                newValue = newValue.replace("{uuid}", UUID.randomUUID().toString());
            }

            copy.add(newKey, newValue);
            results.add(copy.build());
        }

        return results.stream();
    }
}
