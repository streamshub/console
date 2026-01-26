package com.github.streamshub.console.config.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderDefinedContext;
import jakarta.validation.Payload;

public class ResourceTypes {

    public interface ResourceType<E> {
        String value();

        default Set<ResourceType<E>> expand() {
            return Set.of(this);
        }
    }

    public static <E extends ResourceType<E>> E forValue(String value, Class<E> type) {
        for (var v : type.getEnumConstants()) {
            if (v.value().equals(value)) {
                return v;
            }
        }
        return null;
    }

    public enum Global implements ResourceType<Global> {
        KAFKAS("kafkas"),
        ALL("*") {
            @Override
            public Set<ResourceType<Global>> expand() {
                return ALL_EXPANDED;
            }
        };

        private static final Set<ResourceType<Global>> ALL_EXPANDED = Arrays.stream(Global.values())
                .filter(Predicate.not(ALL::equals))
                .collect(Collectors.toSet());

        private String value;

        private Global(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum Kafka implements ResourceType<Kafka> {
        CONSUMER_GROUPS("consumerGroups"),
        NODE_CONFIGS("nodes/configs"),
        NODES("nodes"),
        NODE_METRICS("nodes/metrics"),
        REBALANCES("rebalances"),
        TOPICS("topics"),
        TOPIC_RECORDS("topics/records"),
        TOPIC_METRICS("topics/metrics"),
        USERS("users"),
        ALL("*") {
            @Override
            public Set<ResourceType<Kafka>> expand() {
                return ALL_EXPANDED;
            }
        };

        private static final Set<ResourceType<Kafka>> ALL_EXPANDED = Arrays.stream(Kafka.values())
                .filter(Predicate.not(ALL::equals))
                .collect(Collectors.toSet());

        private String value;

        private Kafka(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidResourceTypes.Validator.class)
    @Documented
    public @interface ValidResourceTypes {
        static final String MESSAGE = "Invalid resource";

        String message() default MESSAGE;

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        @SuppressWarnings("rawtypes")
        Class<? extends ResourceType> type();

        static class Validator implements ConstraintValidator<ValidResourceTypes, SecurityConfig> {
            @SuppressWarnings("rawtypes")
            private Class<? extends ResourceType> type;

            @Override
            public void initialize(ValidResourceTypes constraintAnnotation) {
                this.type = constraintAnnotation.type();
            }

            @Override
            public boolean isValid(SecurityConfig value, ConstraintValidatorContext context) {
                AtomicBoolean valid = new AtomicBoolean(true);
                int i = -1;

                for (var role : value.getRoles()) {
                    i++;
                    int j = -1;
                    for (var rule : role.getRules()) {
                        j++;

                        // Setup a violation builder in case it is needed
                        var builder = context.buildConstraintViolationWithTemplate(MESSAGE)
                            .addContainerElementNode("roles", List.class, 0)
                            .addContainerElementNode("rules", List.class, 0)
                                .inIterable().atIndex(i)
                            .addContainerElementNode("resources", List.class, 0)
                                .inIterable().atIndex(j);

                        validate(rule, context, valid, builder);
                    }
                }

                i = -1;
                for (var auditRule : value.getAudit()) {
                    i++;

                    // Setup a violation builder in case it is needed
                    var builder = context.buildConstraintViolationWithTemplate(MESSAGE)
                        .addContainerElementNode("audit", List.class, 0)
                        .addContainerElementNode("resources", List.class, 0)
                            .inIterable().atIndex(i);

                    validate(auditRule, context, valid, builder);
                }

                return valid.get();
            }

            @SuppressWarnings("unchecked")
            private void validate(RuleConfig value, ConstraintValidatorContext context, AtomicBoolean valid, ContainerElementNodeBuilderDefinedContext builder) {
                int i = -1;

                for (var resource : value.getResources()) {
                    i++;
                    if (ResourceTypes.forValue(resource, type) == null) {
                        valid.set(false);
                        context.disableDefaultConstraintViolation();

                        builder.addContainerElementNode("", List.class, 0)
                            .inIterable()
                            .atIndex(i);

                        builder.addConstraintViolation();
                    }
                }

            }
        }
    }
}
