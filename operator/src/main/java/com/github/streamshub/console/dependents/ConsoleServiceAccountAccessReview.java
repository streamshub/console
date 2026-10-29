package com.github.streamshub.console.dependents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Credentials;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Reasons;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.api.v1alpha1.status.ConsoleStatus;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.authorization.v1.SubjectAccessReviewBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.strimzi.api.kafka.model.user.KafkaUser;

import static com.github.streamshub.console.dependents.support.ConfigSupport.getResource;

/**
 * Virtual dependent resource (creates no Kubernetes resources) that performs a
 * SubjectAccessReview for every KafkaUser credential Secret referenced in the Console
 * spec, verifying whether the per-Console ServiceAccount has been granted {@code get}
 * access on each one.
 *
 * <p>This resource sits between {@link ConsoleServiceAccount} and
 * {@link ConfigurationProcessor} in the workflow DAG. The {@code dependsOn
 * ConsoleServiceAccount} guarantee means the ServiceAccount exists before any SAR is
 * evaluated, avoiding false denials on first reconciliation.
 *
 * <p>All referenced Secrets are checked before the result is determined. The configured
 * denial action controls the result: {@code PRESERVE} sets a {@code SecurityError}
 * condition and blocks downstream configuration updates, {@code REMOVE} sets a
 * {@code SecurityWarning} condition and allows denied Kafka clusters to be omitted from
 * generated configuration, and {@code IGNORE} proceeds with denied credentials. The
 * default is {@code PRESERVE}; {@code IGNORE} is an explicit security bypass.
 */
@ApplicationScoped
public class ConsoleServiceAccountAccessReview extends BaseVirtualResource {

    public static final String NAME = "ConsoleServiceAccountAccessReview"; // NOSONAR
    public static final String DENIED_ACTION = "console.service-account.access-review.denied-action";
    public static final String ALLOWED_USERS = NAME + ".ALLOWED";

    private static final Logger LOGGER = Logger.getLogger(ConsoleServiceAccountAccessReview.class);

    public enum AccessReviewDeniedAction {
        /**
         * Ignore (do not enforce) a subject access review for the Console's service
         * account. Referenced KafkaUser secret information will be copied into the
         * Console instance configuration Secret regardless of whether the service
         * account has been granted access explicitly.
         */
        IGNORE,
        /**
         * Sets a {@code SecurityError} condition and halts all further reconciliation of the
         * Console custom resource. Previously-created owned resources (secret,
         * deployment, etc.) will be left unchanged from their previous states.
         */
        PRESERVE,
        /**
         * Sets a {@code SecurityWarning} condition and proceeds with reconciliation of
         * the Console custom resource without the denied credentials. Pre-existing
         * console instances will have Kafka cluster connections removed if they utilize
         * secret information that is not granted to the service account.
         */
        REMOVE;

        static AccessReviewDeniedAction fromValue(String value) {
            for (var entry : values()) {
                if (entry.name().equalsIgnoreCase(value)) {
                    return entry;
                }
            }

            throw new IllegalArgumentException("""
                    Invalid configuration value for `%s`. Received: `%s`. Expected one of: %s
                    """.formatted(DENIED_ACTION, value, Arrays.asList(values())));
        }
    }

    @Inject
    @ConfigProperty(name = DENIED_ACTION)
    String accessReviewDeniedAction;

    @Inject
    ConsoleServiceAccount serviceAccount;

    private AccessReviewDeniedAction deniedAction;

    public static class Postcondition implements Condition<HasMetadata, Console> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            return context.managedWorkflowAndDependentResourceContext().getMandatory(NAME, Boolean.class);
        }
    }

    public ConsoleServiceAccountAccessReview() {
        super(NAME);
    }

    @PostConstruct
    void initDeniedAction() {
        deniedAction = AccessReviewDeniedAction.fromValue(accessReviewDeniedAction);
    }

    @Override
    public ReconcileResult<HasMetadata> reconcile(Console primary, Context<Console> context) {
        try {
            var deniedByNamespace = checkDeniedSecrets(primary, context);

            if (deniedByNamespace.isEmpty()) {
                // Nothing is denied - proceed with CR reconciliation
                context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.TRUE);
            } else {
                switch (deniedAction) {
                    case IGNORE:
                        logDenialIgnored(primary, deniedByNamespace);
                        context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.TRUE);
                        break;
                    case PRESERVE:
                        setCondition(primary, deniedByNamespace, Types.SECURITY_ERROR);
                        context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.FALSE);
                        break;
                    case REMOVE:
                        setCondition(primary, deniedByNamespace, Types.SECURITY_WARNING);
                        context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.TRUE);
                        break;
                }
            }
        } catch (Exception e) {
            if (!(e instanceof ReconciliationException)) {
                LOGGER.warnf(e,
                        "Exception processing console configuration from %s/%s",
                        primary.getMetadata().getNamespace(),
                        primary.getMetadata().getName());
            }

            primary.getOrCreateStatus()
                    .updateCondition(new ConditionBuilder()
                    .withType(Types.ERROR)
                    .withStatus("True")
                    .withLastTransitionTime(Instant.now().toString())
                    .withReason(Reasons.RECONCILIATION_EXCEPTION)
                    .withMessage(e.getMessage())
                    .build());

            context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.FALSE);
        }

        return ReconcileResult.noOperation(primary);
    }

    /**
     * Collect denied Secrets grouped by namespace. One Role+RoleBinding per namespace
     * covers all denied Secrets in that namespace, minimizing RBAC objects.
     * LinkedHashMap preserves insertion order for deterministic error messages.
     */
    private Map<String, List<String>> checkDeniedSecrets(Console primary, Context<Console> context) {
        String consoleNs = primary.getMetadata().getNamespace();
        String saName = serviceAccount.instanceName(primary);
        Map<String, List<String>> deniedByNamespace = new LinkedHashMap<>();
        Map<String, Map<String, KafkaUser>> allowedUsers = new HashMap<>();

        for (var kafkaRef : coalesce(primary.getSpec().getKafkaClusters(), Collections::emptyList)) {
            var user = Optional.ofNullable(kafkaRef.getCredentials())
                    .map(Credentials::getKafkaUser)
                    .orElse(null);

            if (user == null) {
                continue;
            }

            String userNs = Optional.ofNullable(user.getNamespace()).orElse(kafkaRef.getNamespace());
            KafkaUser kafkaUser = getResource(context, KafkaUser.class, userNs, user.getName());
            String secretName = ConfigurationProcessor.getSecretName(kafkaUser);

            if (hasSecretAccess(context, consoleNs, saName, userNs, secretName)) {
                retainUser(allowedUsers, userNs, user.getName(), kafkaUser);
            } else {
                deniedByNamespace.computeIfAbsent(userNs, k -> new ArrayList<>()).add(secretName);

                if (deniedAction == AccessReviewDeniedAction.IGNORE) {
                    retainUser(allowedUsers, userNs, user.getName(), kafkaUser);
                }
            }
        }

        // Keep the valid users in the reconciliation context for downstream processing
        setAttribute(context, ALLOWED_USERS, allowedUsers);

        return deniedByNamespace;
    }

    private void retainUser(Map<String, Map<String, KafkaUser>> users, String namespace, String name, KafkaUser user) {
        users.computeIfAbsent(namespace, k -> new HashMap<>()).put(name, user);
    }

    private boolean hasSecretAccess(Context<Console> context,
            String consoleNs, String saName, String secretNs, String secretName) {

        var review = context.getClient()
                .authorization()
                .v1()
                .subjectAccessReview()
                .create(new SubjectAccessReviewBuilder()
                        .withNewSpec()
                            .withNewResourceAttributes()
                                .withNamespace(secretNs)
                                .withVerb("get")
                                .withResource("secrets")
                                .withName(secretName)
                            .endResourceAttributes()
                            .withUser("system:serviceaccount:%s:%s".formatted(consoleNs, saName))
                            .withGroups("system:serviceaccounts", "system:serviceaccounts:" + consoleNs)
                        .endSpec()
                        .build());

        return Boolean.TRUE.equals(review.getStatus().getAllowed());
    }

    private void logDenialIgnored(Console primary, Map<String, List<String>> deniedByNamespace) {
        String consoleNs = primary.getMetadata().getNamespace();
        String saName = serviceAccount.instanceName(primary);

        LOGGER.debugf("""
                Ignoring denied access. Console ServiceAccount '%s/%s' does not have 'get' \
                permission on the following Secrets: %s""", consoleNs, saName, formatSecretList(deniedByNamespace));
    }

    private void setCondition(Console primary, Map<String, List<String>> deniedByNamespace, String type) {
        String consoleNs = primary.getMetadata().getNamespace();
        String saName = serviceAccount.instanceName(primary);

        // One role/rolebinding command pair per namespace — --resource-name is repeated
        // once per Secret name (the flag does not accept CSV values).
        String remediation = deniedByNamespace.entrySet().stream()
                .map(e -> {
                    String ns = e.getKey();
                    String resourceNameFlags = e.getValue().stream()
                            .map(s -> "--resource-name=" + s)
                            .collect(Collectors.joining(" "));
                    return "kubectl create role console-kafkauser-secret-reader"
                            + " --namespace=" + ns
                            + " --verb=get --resource=secrets"
                            + " " + resourceNameFlags
                            + " && kubectl create rolebinding console-kafkauser-secret-reader"
                            + " --namespace=" + ns
                            + " --role=console-kafkauser-secret-reader"
                            + " --serviceaccount=" + consoleNs + ":" + saName;
                })
                .collect(Collectors.joining(" ; "));

        ConsoleStatus status = primary.getOrCreateStatus();

        status.updateCondition(new ConditionBuilder()
                .withType(type)
                .withStatus("True")
                .withLastTransitionTime(Instant.now().toString())
                .withReason(Reasons.ACCESS_DENIED)
                .withMessage("""
                        Console ServiceAccount '%s/%s' does not have 'get' permission \
                        on the following Secrets: %s. \
                        Grant access with: %s"""
                        .formatted(consoleNs, saName, formatSecretList(deniedByNamespace), remediation))
                .build());
    }

    private String formatSecretList(Map<String, List<String>> secretsByNamespace) {
        return secretsByNamespace.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(s -> "'%s/%s'".formatted(e.getKey(), s)))
            .collect(Collectors.joining(", "));
    }
}
