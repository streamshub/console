package com.github.streamshub.console.dependents.support;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.streamshub.console.api.v1alpha1.spec.security.LdapIdentityProvider;
import com.github.streamshub.console.api.v1alpha1.spec.security.OpenshiftIdentityProvider;

/**
 * Root of the Dex {@code config.yaml} and host for all nested mapping types.
 *
 * <p>Use {@link #forOpenshift} or {@link #forLdap} to construct an instance
 * from operator CR fields.
 *
 * @see <a href="https://dexidp.io/docs/configuration/">Dex configuration</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DexConfig(
        String issuer,
        Storage storage,
        Web web,
        OAuth2 oauth2,
        List<Connector> connectors,
        List<StaticClient> staticClients) {

    public static final String DEX_TLS_MOUNT_PATH = "/etc/dex/tls"; // NOSONAR
    private static final String DEX_BASE = "https://%s";
    private static final String DEX_PATH = "/dex";

    // -------------------------------------------------------------------------
    // Nested mapping types (no Dex prefix — scoped to this class)
    // -------------------------------------------------------------------------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Storage(String type, StorageConfig config) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StorageConfig(boolean inCluster) {
    }

    /**
     * Dex web server settings. Either {@code http} or {@code https}
     * (with {@code tlsCert} and {@code tlsKey}) is set.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Web(
            String http,
            String https,
            String tlsCert,
            String tlsKey,
            String pathPrefix) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OAuth2(List<String> responseTypes, boolean skipApprovalScreen) {
    }

    /**
     * A Dex connector. {@code config} is specific to the connector {@code type}:
     * either {@link OpenshiftConnectorConfig} or {@link LdapConnectorConfig}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Connector(String type, String id, String name, Object config) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpenshiftConnectorConfig(
            String issuer,
            String clientID,
            String clientSecretFile,
            String redirectURI,
            Boolean insecureCA,
            String rootCA,
            List<String> groups) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LdapConnectorConfig(
            String host,
            Boolean insecureNoSSL,
            Boolean insecureSkipVerify,
            Boolean startTLS,
            String bindDN,
            String bindPW,
            LdapUserSearch userSearch,
            LdapGroupSearch groupSearch) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LdapUserSearch(
            String baseDN,
            String filter,
            String username,
            String scope,
            String idAttr,
            String emailAttr,
            String nameAttr,
            String preferredUsernameAttr) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserMatcher(String userAttr, String groupAttr) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LdapGroupSearch(
            String baseDN,
            String filter,
            String scope,
            List<UserMatcher> userMatchers,
            String nameAttr) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StaticClient(
            String id,
            @JsonProperty("public") boolean isPublic,
            String name,
            List<String> redirectURIs) {
    }

    // -------------------------------------------------------------------------
    // Static factories
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link DexConfig} for the OpenShift connector.
     *
     * @param host         the public hostname of the Console (e.g. {@code example.com})
     * @param openShiftTls {@code true} when running on OpenShift — switches Dex's web
     *                     listener to HTTPS using the service CA serving certificate
     * @param openshift    the {@link OpenshiftIdentityProvider} from the Console CR
     * @param apiServerUrl the OpenShift API server URL (used as the connector issuer);
     *                     caller is responsible for stripping any trailing slash
     * @param saClientId   the fully-qualified ServiceAccount name used as the OAuth
     *                     client ID ({@code system:serviceaccount:<ns>:<name>})
     */
    public static DexConfig forOpenshift(
            String host,
            boolean openShiftTls,
            OpenshiftIdentityProvider openshift,
            String apiServerUrl,
            String saClientId) {

        boolean insecureCA = Boolean.TRUE.equals(openshift.isInsecureCA());
        // With no explicit CA provided, use the cluster root CA mounted from kube-root-ca.crt
        String rootCA = !insecureCA && openshift.getTrustStore() == null ? "/etc/ssl/openshift.pem" : null;
        List<String> groups = Optional.ofNullable(openshift.getGroups())
                .filter(Predicate.not(List::isEmpty))
                .orElse(null);

        var connectorConfig = new OpenshiftConnectorConfig(
                apiServerUrl,
                saClientId,
                "/var/run/secrets/kubernetes.io/serviceaccount/token",
                DEX_BASE.formatted(host) + DEX_PATH + "/callback",
                insecureCA ? Boolean.TRUE : null,
                rootCA,
                groups);

        return build(host, openShiftTls, new Connector("openshift", "openshift", "OpenShift", connectorConfig));
    }

    /**
     * Builds a {@link DexConfig} for the LDAP connector.
     *
     * @param host   the public hostname of the Console
     * @param ldap   the {@link LdapIdentityProvider} from the Console CR
     * @param bindPW the bind password resolved from the CR secret reference, or
     *               {@code null} if no bind secret was configured
     */
    public static DexConfig forLdap(String host, LdapIdentityProvider ldap, String bindPW) {
        var userSearch = Optional.ofNullable(ldap.getUserSearch())
                .map(s -> new LdapUserSearch(
                        s.getBaseDN(), s.getFilter(), s.getUsername(),
                        s.getScope(), s.getIdAttr(), s.getEmailAttr(), s.getNameAttr(),
                        s.getPreferredUsernameAttr()))
                .orElse(null);

        var groupSearch = Optional.ofNullable(ldap.getGroupSearch())
                .map(s -> {
                    List<UserMatcher> userMatchers = Optional.ofNullable(s.getUserMatchers())
                            .map(matchers -> matchers.stream()
                                    .map(m -> new UserMatcher(m.getUserAttr(), m.getGroupAttr()))
                                    .toList())
                            .orElse(null);
                    return new LdapGroupSearch(
                            s.getBaseDN(), s.getFilter(), s.getScope(),
                            userMatchers, s.getNameAttr());
                })
                .orElse(null);

        var connectorConfig = new LdapConnectorConfig(
                ldap.getHost(),
                ldap.isInsecureNoSSL(),
                ldap.isInsecureSkipVerify(),
                ldap.isStartTLS(),
                ldap.getBindDN(),
                bindPW,
                userSearch,
                groupSearch);

        return build(host, false, new Connector("ldap", "ldap", "LDAP", connectorConfig));
    }

    // -------------------------------------------------------------------------
    // Shared assembly
    // -------------------------------------------------------------------------

    private static DexConfig build(String host, boolean openShiftTls, Connector connector) {
        // On OpenShift the service CA controller issues a serving certificate mounted at
        // /etc/dex/tls by DexDeployment, so switch the listener to HTTPS.
        var web = openShiftTls
                ? new Web(null, "0.0.0.0:5556", DEX_TLS_MOUNT_PATH + "/tls.crt", DEX_TLS_MOUNT_PATH + "/tls.key", DEX_PATH)
                : new Web("0.0.0.0:5556", null, null, null, DEX_PATH);

        // The client is declared public so no client secret is needed; PKCE secures the flow.
        // The redirect URI path matches the path used in the Console API's OidcTenantConfigResolver.
        var staticClient = new StaticClient(
                "streamshub-console",
                true,
                "StreamsHub Console",
                List.of(DEX_BASE.formatted(host) + '/'));

        return new DexConfig(
                DEX_BASE.formatted(host) + DEX_PATH,
                new Storage("kubernetes", new StorageConfig(true)),
                web,
                new OAuth2(List.of("code"), true),
                List.of(connector),
                List.of(staticClient));
    }
}
