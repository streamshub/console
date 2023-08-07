package com.github.eyefloaters.console.test;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.kafka.systemtest.utils.ClientsConfig;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

public class TestHelper {

    static final Logger log = Logger.getLogger(TestHelper.class);
    final Config config;
    final String token;
    final Properties adminConfig;

    public TestHelper(URI bootstrapServers, Config config, String token) {
        this.config = config;
        this.token = token;

        adminConfig = token != null ?
            ClientsConfig.getAdminConfigOauth(config, token) :
            ClientsConfig.getAdminConfig(config);

        adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
    }

    public String getClusterId() {
        try (Admin admin = Admin.create(adminConfig)) {
            return admin.describeCluster().clusterId().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }

        return null;
    }

    public static ValidatableResponse whenRequesting(Function<RequestSpecification, Response> requestFn) {
        RequestSpecification requestSpec = given().log().ifValidationFails();
        Response response = requestFn.apply(requestSpec);
        return response.then().log().ifValidationFails();
    }
}
