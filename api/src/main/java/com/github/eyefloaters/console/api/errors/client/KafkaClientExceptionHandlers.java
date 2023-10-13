package com.github.eyefloaters.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidReplicaAssignmentException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import com.github.eyefloaters.console.api.support.ErrorCategory;

public class KafkaClientExceptionHandlers {

    private KafkaClientExceptionHandlers() {
    }

    @Provider
    @ApplicationScoped
    public static class InvalidConfigurationExceptionHandler
        extends AbstractClientExceptionHandler<InvalidConfigurationException> {

        public InvalidConfigurationExceptionHandler() {
            super(ErrorCategory.InvalidResource.class, null, null);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof InvalidConfigurationException;
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
