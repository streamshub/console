package com.github.streamshub.console.api.errors.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.GroupSubscribedToTopicException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidGroupIdException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicaAssignmentException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.NoReassignmentInProgressException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import com.github.streamshub.console.api.support.ErrorCategory;

public class KafkaClientExceptionHandlers {

    private KafkaClientExceptionHandlers() {
    }

    @Provider
    @ApplicationScoped
    public static class AuthenticationExceptionHandler
        extends AbstractClientExceptionHandler<AuthenticationException> {

        public AuthenticationExceptionHandler() {
            super(ErrorCategory.NotAuthenticated.class, null, (String) null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof AuthenticationException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class AuthorizationExceptionHandler
        extends AbstractClientExceptionHandler<AuthorizationException> {

        public AuthorizationExceptionHandler() {
            super(ErrorCategory.NotAuthorized.class, null, (String) null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof AuthorizationException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class InvalidGroupIdExceptionHandler
        extends AbstractClientExceptionHandler<InvalidGroupIdException> {

        public InvalidGroupIdExceptionHandler() {
            super(ErrorCategory.UncategorizedBadRequest.class, null, (String) null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidGroupIdException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class GroupIdNotFoundExceptionHandler
        extends AbstractNotFoundExceptionHandler<GroupIdNotFoundException> {

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof GroupIdNotFoundException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class GroupNotEmptyExceptionHandler
        extends AbstractClientExceptionHandler<GroupNotEmptyException> {

        public GroupNotEmptyExceptionHandler() {
            super(ErrorCategory.ResourceConflict.class, "The consumer group is not empty", (String) null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof GroupNotEmptyException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class GroupSubscribedToTopicExceptionHandler
        extends AbstractClientExceptionHandler<GroupSubscribedToTopicException> {

        public GroupSubscribedToTopicExceptionHandler() {
            super(ErrorCategory.ResourceConflict.class, "The consumer group is not empty", (String) null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof GroupSubscribedToTopicException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class InvalidConfigurationExceptionHandler
        extends AbstractClientExceptionHandler<InvalidConfigurationException> {

        /**
         * Pattern for a "best effort" attempt to identify the invalid property
         * being reported by Kafka. The format searched for here is set by
         * {@linkplain org.apache.kafka.common.config.ConfigException}.
         */
        static final Pattern NAME_SEARCH = Pattern.compile(" for configuration ([^:]+)(?::|$)");
        static final String VALUE_PATH = "/data/attributes/configs/%s/value"; // NOSONAR - not a URL

        public InvalidConfigurationExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, exception -> {
                Matcher m = NAME_SEARCH.matcher(exception.getMessage());
                return m.find() ? VALUE_PATH.formatted(m.group(1)) : null;
            });
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidConfigurationException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class InvalidPartitionsExceptionHandler
        extends AbstractClientExceptionHandler<InvalidPartitionsException> {

        public InvalidPartitionsExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, "/data/attributes/partition");
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidPartitionsException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class InvalidReplicaAssignmentExceptionHandler
        extends AbstractClientExceptionHandler<InvalidReplicaAssignmentException> {

        public InvalidReplicaAssignmentExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, "/data/attributes/replicasAssignments");
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidReplicaAssignmentException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class InvalidReplicationFactorExceptionHandler
        extends AbstractClientExceptionHandler<InvalidReplicationFactorException> {

        public InvalidReplicationFactorExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, "/data/attributes/replicationFactor");
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidReplicationFactorException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class TopicExistsExceptionHandler
        extends AbstractClientExceptionHandler<TopicExistsException> {

        public TopicExistsExceptionHandler() {
            super(ErrorCategory.ResourceConflict.class, null, "/data/attributes/name");
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof TopicExistsException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class NoReassignmentInProgressExceptionHandler
        extends AbstractClientExceptionHandler<NoReassignmentInProgressException> {

        public NoReassignmentInProgressExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, "/data/attributes/replicasAssignments");
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof NoReassignmentInProgressException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class UnknownTopicIdExceptionHandler
        extends AbstractNotFoundExceptionHandler<UnknownTopicIdException> {

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof UnknownTopicIdException;
        }
    }

    @Provider
    @ApplicationScoped
    public static class UnknownTopicOrPartitionExceptionHandler
        extends AbstractNotFoundExceptionHandler<UnknownTopicOrPartitionException> {

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof UnknownTopicOrPartitionException;
        }
    }

}
