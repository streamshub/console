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
    /** Key in the Dex Secret holding a user-supplied CA bundle (PEM) for the upstream identity provider. */
    public static final String CA_KEY = "ca.pem";
    /** The Dex Secret is mounted at {@code /etc/dex/cfg}, so the CA bundle is available at this path. */
    public static final String CA_PATH = "/etc/dex/cfg/" + CA_KEY; // NOSONAR
    /** Keys in the Dex Secret holding the LDAP bind credentials, exposed to Dex as environment variables. */
    public static final String LDAP_BIND_DN_KEY = "ldap-bind-dn";
    public static final String LDAP_BIND_PW_KEY = "ldap-bind-pw";
    public static final String LDAP_BIND_DN_ENV = "LDAP_BIND_DN";
    public static final String LDAP_BIND_PW_ENV = "LDAP_BIND_PW";
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
    public record OAuth2(List<String> responseTypes, boolean skipApprovalScreen, Pkce pkce) {
    }

    /**
     * Dex PKCE settings, available since Dex 2.46.0.
     *
     * @param enforce                       reject authorization requests without a {@code code_challenge}
     * @param codeChallengeMethodsSupported accepted challenge methods
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Pkce(boolean enforce, List<String> codeChallengeMethodsSupported) {
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
            String rootCA,
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
     * Builds a {@link DexConfig} for the OpenShift connector. The connector is only
     * supported on OpenShift, so Dex's web listener is always configured for HTTPS using
     * the service CA serving certificate.
     *
     * @param host         the public hostname of the Console (e.g. {@code example.com})
     * @param openshift    the {@link OpenshiftIdentityProvider} from the Console CR
     * @param apiServerUrl the OpenShift API server URL (used as the connector issuer);
     *                     caller is responsible for stripping any trailing slash
     * @param saClientId   the fully-qualified ServiceAccount name used as the OAuth
     *                     client ID ({@code system:serviceaccount:<ns>:<name>})
     * @param customCA     {@code true} when the CR trust store was added to the Dex Secret
     *                     at {@link #CA_PATH}
     */
    public static DexConfig forOpenshift(
            String host,
            OpenshiftIdentityProvider openshift,
            String apiServerUrl,
            String saClientId,
            boolean customCA) {

        boolean insecureCA = Boolean.TRUE.equals(openshift.isInsecureCA());
        // With no explicit CA provided, use the cluster root CA mounted from kube-root-ca.crt
        String rootCA = null;

        if (!insecureCA) {
            rootCA = customCA ? CA_PATH : "/etc/ssl/openshift.pem";
        }
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

        return build(host, true, new Connector("openshift", "openshift", "OpenShift", connectorConfig));
    }

    /**
     * Builds a {@link DexConfig} for the LDAP connector.
     *
     * <p>The bind DN and password are never written to the config. Dex expands
     * environment variables in its config values, and the expansion is not applied to
     * the variable's value, so a reference such as {@code ${LDAP_BIND_PW}} is safe for
     * values containing {@code $}, whereas a literal value would be mangled. The
     * variables are populated from the Dex Secret by {@code DexDeployment}.
     *
     * @param host   the public hostname of the Console
     * @param ldap   the {@link LdapIdentityProvider} from the Console CR
     * @param hasBindPW {@code true} when a bind password was resolved and stored in the
     *               Dex Secret under {@link #LDAP_BIND_PW_KEY}
     * @param customCA {@code true} when the CR trust store was added to the Dex Secret
     *               at {@link #CA_PATH}
     */
    public static DexConfig forLdap(String host, LdapIdentityProvider ldap, boolean hasBindPW, boolean customCA) {
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
                customCA ? CA_PATH : null,
                ldap.getBindDN() != null ? "${" + LDAP_BIND_DN_ENV + "}" : null,
                hasBindPW ? "${" + LDAP_BIND_PW_ENV + "}" : null,
                userSearch,
                groupSearch);

        return build(host, false, new Connector("ldap", "ldap", "LDAP", connectorConfig));
    }

    // -------------------------------------------------------------------------
    // Shared assembly
    // -------------------------------------------------------------------------

    private static DexConfig build(String host, boolean tls, Connector connector) {
        // On OpenShift the service CA controller issues a serving certificate mounted at
        // /etc/dex/tls by DexDeployment, so switch the listener to HTTPS.
        var web = tls
                ? new Web(null, "0.0.0.0:5556", DEX_TLS_MOUNT_PATH + "/tls.crt", DEX_TLS_MOUNT_PATH + "/tls.key", DEX_PATH)
                : new Web("0.0.0.0:5556", null, null, null, DEX_PATH);

        // The client is declared public so no client secret is needed; PKCE secures the flow
        // and is enforced by Dex (see the oauth2.pkce configuration below).
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
                // S256 only: the Console (Quarkus OIDC) uses S256, and "plain" offers no protection
                // against an intercepted authorization request.
                new OAuth2(List.of("code"), true, new Pkce(true, List.of("S256"))),
                List.of(connector),
                List.of(staticClient));
    }
}
