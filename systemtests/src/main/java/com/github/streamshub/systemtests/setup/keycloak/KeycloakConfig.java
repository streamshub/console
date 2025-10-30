
package com.github.streamshub.systemtests.setup.keycloak;

public class KeycloakConfig {
    private final String username;
    private final String password;
    private final String namespace;
    private final String httpsUri;

    private String clientSecret;

    public KeycloakConfig(String namespace, String password, String username) {

        this.username = username;
        this.password = password;
        this.namespace = namespace;
        this.httpsUri = "https://keycloak-service." + namespace + ".svc.cluster.local:8443";
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getHttpsUri() {
        return httpsUri;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    // Setters
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
