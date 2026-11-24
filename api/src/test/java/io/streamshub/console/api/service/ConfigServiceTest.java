package io.streamshub.console.api.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.streamshub.console.api.model.ConfigEntry;
import io.streamshub.console.api.support.KafkaContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigServiceTest {

    private static final ConfigResource BROKER = new ConfigResource(ConfigResource.Type.BROKER, "0");

    @Test
    void testDocumentationNotRequestedByDefault() throws Exception {
        Result result = describeConfigs(false);

        assertFalse(result.options().includeDocumentation());
        assertNull(result.configs().get("log.dirs").getDocumentation());
    }

    @Test
    void testDocumentationRequestedWhenEnabled() throws Exception {
        Result result = describeConfigs(true);

        assertTrue(result.options().includeDocumentation());
        assertEquals("documentation", result.configs().get("log.dirs").getDocumentation());
    }

    @Test
    void testTypeNameWithExplicitFalseDoesNotRequestDocumentation() throws Exception {
        Admin admin = mock(Admin.class);
        ArgumentCaptor<DescribeConfigsOptions> options = stubDescribeConfigs(admin, false);
        KafkaContext kafkaContext = mock(KafkaContext.class);
        when(kafkaContext.admin()).thenReturn(admin);

        ConfigService configService = new ConfigService();
        configService.kafkaContext = kafkaContext;
        configService.describeConfigs(ConfigResource.Type.BROKER, "0", false)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertFalse(options.getValue().includeDocumentation());
    }

    @Test
    void testAdminWithExplicitFalseDoesNotRequestDocumentation() throws Exception {
        Admin admin = mock(Admin.class);
        ArgumentCaptor<DescribeConfigsOptions> options = stubDescribeConfigs(admin, false);

        new ConfigService()
            .describeConfigs(admin, List.of(BROKER), false)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertFalse(options.getValue().includeDocumentation());
    }

    Result describeConfigs(boolean includeDocumentation) throws Exception {
        Admin admin = mock(Admin.class);
        ArgumentCaptor<DescribeConfigsOptions> options = stubDescribeConfigs(admin, includeDocumentation);

        var response = new ConfigService()
                .describeConfigs(admin, List.of(BROKER), includeDocumentation)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Map<String, ConfigEntry> configs = response.get("0").getOrThrow(CompletionException::new);
        return new Result(configs, options.getValue());
    }

    ArgumentCaptor<DescribeConfigsOptions> stubDescribeConfigs(Admin admin, boolean includeDocumentation) {
        var kafkaEntry = new org.apache.kafka.clients.admin.ConfigEntry(
                "log.dirs",
                "/tmp",
                org.apache.kafka.clients.admin.ConfigEntry.ConfigSource.DEFAULT_CONFIG,
                false,
                false,
                List.of(),
                org.apache.kafka.clients.admin.ConfigEntry.ConfigType.STRING,
                includeDocumentation ? "documentation" : null);
        var config = new Config(List.of(kafkaEntry));
        DescribeConfigsResult describeResult = mock(DescribeConfigsResult.class);
        when(describeResult.values()).thenReturn(Map.of(BROKER, KafkaFuture.completedFuture(config)));

        ArgumentCaptor<DescribeConfigsOptions> options = ArgumentCaptor.forClass(DescribeConfigsOptions.class);
        when(admin.describeConfigs(eq(List.of(BROKER)), options.capture())).thenReturn(describeResult);
        return options;
    }

    record Result(Map<String, ConfigEntry> configs, DescribeConfigsOptions options) {
    }
}
