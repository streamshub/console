import { z } from "zod";

export const kafkaCRD = z.object({
  spec: z
    .object({
      kafka: z
        .object({
          version: z
            .string()
            .describe(
              "The kafka broker version. Defaults to {DefaultKafkaVersion}. Consult the user documentation to understand the process required to upgrade or downgrade the version."
            )
            .optional(),
          replicas: z
            .number()
            .int()
            .gte(1)
            .describe("The number of pods in the cluster."),
          image: z
            .string()
            .describe(
              "The docker image for the pods. The default value depends on the configured `Kafka.spec.kafka.version`."
            )
            .optional(),
          listeners: z
            .array(
              z.object({
                name: z
                  .string()
                  .regex(new RegExp("^[a-z0-9]{1,11}$"))
                  .describe(
                    "Name of the listener. The name will be used to identify the listener and the related Kubernetes objects. The name has to be unique within given a Kafka cluster. The name can consist of lowercase characters and numbers and be up to 11 characters long."
                  ),
                port: z
                  .number()
                  .int()
                  .gte(9092)
                  .describe(
                    "Port number used by the listener inside Kafka. The port number has to be unique within a given Kafka cluster. Allowed port numbers are 9092 and higher with the exception of ports 9404 and 9999, which are already used for Prometheus and JMX. Depending on the listener type, the port number might not be the same as the port number that connects Kafka clients."
                  ),
                type: z
                  .enum([
                    "internal",
                    "route",
                    "loadbalancer",
                    "nodeport",
                    "ingress",
                    "cluster-ip",
                  ])
                  .describe(
                    "Type of the listener. Currently the supported types are `internal`, `route`, `loadbalancer`, `nodeport` and `ingress`. \n\n* `internal` type exposes Kafka internally only within the Kubernetes cluster.\n* `route` type uses OpenShift Routes to expose Kafka.\n* `loadbalancer` type uses LoadBalancer type services to expose Kafka.\n* `nodeport` type uses NodePort type services to expose Kafka.\n* `ingress` type uses Kubernetes Nginx Ingress to expose Kafka with TLS passthrough.\n* `cluster-ip` type uses a per-broker `ClusterIP` service.\n"
                  ),
                tls: z
                  .boolean()
                  .describe(
                    "Enables TLS encryption on the listener. This is a required property."
                  ),
                authentication: z
                  .object({
                    accessTokenIsJwt: z
                      .boolean()
                      .describe(
                        "Configure whether the access token is treated as JWT. This must be set to `false` if the authorization server returns opaque tokens. Defaults to `true`."
                      )
                      .optional(),
                    checkAccessTokenType: z
                      .boolean()
                      .describe(
                        "Configure whether the access token type check is performed or not. This should be set to `false` if the authorization server does not include 'typ' claim in JWT token. Defaults to `true`."
                      )
                      .optional(),
                    checkAudience: z
                      .boolean()
                      .describe(
                        "Enable or disable audience checking. Audience checks identify the recipients of tokens. If audience checking is enabled, the OAuth Client ID also has to be configured using the `clientId` property. The Kafka broker will reject tokens that do not have its `clientId` in their `aud` (audience) claim.Default value is `false`."
                      )
                      .optional(),
                    checkIssuer: z
                      .boolean()
                      .describe(
                        "Enable or disable issuer checking. By default issuer is checked using the value configured by `validIssuerUri`. Default value is `true`."
                      )
                      .optional(),
                    clientAudience: z
                      .string()
                      .describe(
                        "The audience to use when making requests to the authorization server's token endpoint. Used for inter-broker authentication and for configuring OAuth 2.0 over PLAIN using the `clientId` and `secret` method."
                      )
                      .optional(),
                    clientId: z
                      .string()
                      .describe(
                        "OAuth Client ID which the Kafka broker can use to authenticate against the authorization server and use the introspect endpoint URI."
                      )
                      .optional(),
                    clientScope: z
                      .string()
                      .describe(
                        "The scope to use when making requests to the authorization server's token endpoint. Used for inter-broker authentication and for configuring OAuth 2.0 over PLAIN using the `clientId` and `secret` method."
                      )
                      .optional(),
                    clientSecret: z
                      .object({
                        key: z
                          .string()
                          .describe(
                            "The key under which the secret value is stored in the Kubernetes Secret."
                          ),
                        secretName: z
                          .string()
                          .describe(
                            "The name of the Kubernetes Secret containing the secret value."
                          ),
                      })
                      .describe(
                        "Link to Kubernetes Secret containing the OAuth client secret which the Kafka broker can use to authenticate against the authorization server and use the introspect endpoint URI."
                      )
                      .optional(),
                    connectTimeoutSeconds: z
                      .number()
                      .int()
                      .describe(
                        "The connect timeout in seconds when connecting to authorization server. If not set, the effective connect timeout is 60 seconds."
                      )
                      .optional(),
                    customClaimCheck: z
                      .string()
                      .describe(
                        "JsonPath filter query to be applied to the JWT token or to the response of the introspection endpoint for additional token validation. Not set by default."
                      )
                      .optional(),
                    disableTlsHostnameVerification: z
                      .boolean()
                      .describe(
                        "Enable or disable TLS hostname verification. Default value is `false`."
                      )
                      .optional(),
                    enableECDSA: z
                      .boolean()
                      .describe(
                        "Enable or disable ECDSA support by installing BouncyCastle crypto provider. ECDSA support is always enabled. The BouncyCastle libraries are no longer packaged with Strimzi. Value is ignored."
                      )
                      .optional(),
                    enableMetrics: z
                      .boolean()
                      .describe(
                        "Enable or disable OAuth metrics. Default value is `false`."
                      )
                      .optional(),
                    enableOauthBearer: z
                      .boolean()
                      .describe(
                        "Enable or disable OAuth authentication over SASL_OAUTHBEARER. Default value is `true`."
                      )
                      .optional(),
                    enablePlain: z
                      .boolean()
                      .describe(
                        "Enable or disable OAuth authentication over SASL_PLAIN. There is no re-authentication support when this mechanism is used. Default value is `false`."
                      )
                      .optional(),
                    failFast: z
                      .boolean()
                      .describe(
                        "Enable or disable termination of Kafka broker processes due to potentially recoverable runtime errors during startup. Default value is `true`."
                      )
                      .optional(),
                    fallbackUserNameClaim: z
                      .string()
                      .describe(
                        "The fallback username claim to be used for the user id if the claim specified by `userNameClaim` is not present. This is useful when `client_credentials` authentication only results in the client id being provided in another claim. It only takes effect if `userNameClaim` is set."
                      )
                      .optional(),
                    fallbackUserNamePrefix: z
                      .string()
                      .describe(
                        "The prefix to use with the value of `fallbackUserNameClaim` to construct the user id. This only takes effect if `fallbackUserNameClaim` is true, and the value is present for the claim. Mapping usernames and client ids into the same user id space is useful in preventing name collisions."
                      )
                      .optional(),
                    groupsClaim: z
                      .string()
                      .describe(
                        "JsonPath query used to extract groups for the user during authentication. Extracted groups can be used by a custom authorizer. By default no groups are extracted."
                      )
                      .optional(),
                    groupsClaimDelimiter: z
                      .string()
                      .describe(
                        "A delimiter used to parse groups when they are extracted as a single String value rather than a JSON array. Default value is ',' (comma)."
                      )
                      .optional(),
                    httpRetries: z
                      .number()
                      .int()
                      .describe(
                        "The maximum number of retries to attempt if an initial HTTP request fails. If not set, the default is to not attempt any retries."
                      )
                      .optional(),
                    httpRetryPauseMs: z
                      .number()
                      .int()
                      .describe(
                        "The pause to take before retrying a failed HTTP request. If not set, the default is to not pause at all but to immediately repeat a request."
                      )
                      .optional(),
                    introspectionEndpointUri: z
                      .string()
                      .describe(
                        "URI of the token introspection endpoint which can be used to validate opaque non-JWT tokens."
                      )
                      .optional(),
                    jwksEndpointUri: z
                      .string()
                      .describe(
                        "URI of the JWKS certificate endpoint, which can be used for local JWT validation."
                      )
                      .optional(),
                    jwksExpirySeconds: z
                      .number()
                      .int()
                      .gte(1)
                      .describe(
                        "Configures how often are the JWKS certificates considered valid. The expiry interval has to be at least 60 seconds longer then the refresh interval specified in `jwksRefreshSeconds`. Defaults to 360 seconds."
                      )
                      .optional(),
                    jwksIgnoreKeyUse: z
                      .boolean()
                      .describe(
                        "Flag to ignore the 'use' attribute of `key` declarations in a JWKS endpoint response. Default value is `false`."
                      )
                      .optional(),
                    jwksMinRefreshPauseSeconds: z
                      .number()
                      .int()
                      .gte(0)
                      .describe(
                        "The minimum pause between two consecutive refreshes. When an unknown signing key is encountered the refresh is scheduled immediately, but will always wait for this minimum pause. Defaults to 1 second."
                      )
                      .optional(),
                    jwksRefreshSeconds: z
                      .number()
                      .int()
                      .gte(1)
                      .describe(
                        "Configures how often are the JWKS certificates refreshed. The refresh interval has to be at least 60 seconds shorter then the expiry interval specified in `jwksExpirySeconds`. Defaults to 300 seconds."
                      )
                      .optional(),
                    listenerConfig: z
                      .record(z.any())
                      .describe(
                        "Configuration to be used for a specific listener. All values are prefixed with listener.name._<listener_name>_."
                      )
                      .optional(),
                    maxSecondsWithoutReauthentication: z
                      .number()
                      .int()
                      .describe(
                        "Maximum number of seconds the authenticated session remains valid without re-authentication. This enables Apache Kafka re-authentication feature, and causes sessions to expire when the access token expires. If the access token expires before max time or if max time is reached, the client has to re-authenticate, otherwise the server will drop the connection. Not set by default - the authenticated session does not expire when the access token expires. This option only applies to SASL_OAUTHBEARER authentication mechanism (when `enableOauthBearer` is `true`)."
                      )
                      .optional(),
                    readTimeoutSeconds: z
                      .number()
                      .int()
                      .describe(
                        "The read timeout in seconds when connecting to authorization server. If not set, the effective read timeout is 60 seconds."
                      )
                      .optional(),
                    sasl: z
                      .boolean()
                      .describe("Enable or disable SASL on this listener.")
                      .optional(),
                    secrets: z
                      .array(
                        z.object({
                          key: z
                            .string()
                            .describe(
                              "The key under which the secret value is stored in the Kubernetes Secret."
                            ),
                          secretName: z
                            .string()
                            .describe(
                              "The name of the Kubernetes Secret containing the secret value."
                            ),
                        })
                      )
                      .describe(
                        "Secrets to be mounted to /opt/kafka/custom-authn-secrets/custom-listener-_<listener_name>-<port>_/_<secret_name>_."
                      )
                      .optional(),
                    tlsTrustedCertificates: z
                      .array(
                        z.object({
                          certificate: z
                            .string()
                            .describe(
                              "The name of the file certificate in the Secret."
                            ),
                          secretName: z
                            .string()
                            .describe(
                              "The name of the Secret containing the certificate."
                            ),
                        })
                      )
                      .describe(
                        "Trusted certificates for TLS connection to the OAuth server."
                      )
                      .optional(),
                    tokenEndpointUri: z
                      .string()
                      .describe(
                        "URI of the Token Endpoint to use with SASL_PLAIN mechanism when the client authenticates with `clientId` and a `secret`. If set, the client can authenticate over SASL_PLAIN by either setting `username` to `clientId`, and setting `password` to client `secret`, or by setting `username` to account username, and `password` to access token prefixed with `$accessToken:`. If this option is not set, the `password` is always interpreted as an access token (without a prefix), and `username` as the account username (a so called 'no-client-credentials' mode)."
                      )
                      .optional(),
                    type: z
                      .enum(["tls", "scram-sha-512", "oauth", "custom"])
                      .describe(
                        "Authentication type. `oauth` type uses SASL OAUTHBEARER Authentication. `scram-sha-512` type uses SASL SCRAM-SHA-512 Authentication. `tls` type uses TLS Client Authentication. `tls` type is supported only on TLS listeners.`custom` type allows for any authentication type to be used."
                      ),
                    userInfoEndpointUri: z
                      .string()
                      .describe(
                        "URI of the User Info Endpoint to use as a fallback to obtaining the user id when the Introspection Endpoint does not return information that can be used for the user id. "
                      )
                      .optional(),
                    userNameClaim: z
                      .string()
                      .describe(
                        "Name of the claim from the JWT authentication token, Introspection Endpoint response or User Info Endpoint response which will be used to extract the user id. Defaults to `sub`."
                      )
                      .optional(),
                    validIssuerUri: z
                      .string()
                      .describe(
                        "URI of the token issuer used for authentication."
                      )
                      .optional(),
                    validTokenType: z
                      .string()
                      .describe(
                        "Valid value for the `token_type` attribute returned by the Introspection Endpoint. No default value, and not checked by default."
                      )
                      .optional(),
                  })
                  .describe("Authentication configuration for this listener.")
                  .optional(),
                configuration: z
                  .object({
                    brokerCertChainAndKey: z
                      .object({
                        certificate: z
                          .string()
                          .describe(
                            "The name of the file certificate in the Secret."
                          ),
                        key: z
                          .string()
                          .describe(
                            "The name of the private key in the Secret."
                          ),
                        secretName: z
                          .string()
                          .describe(
                            "The name of the Secret containing the certificate."
                          ),
                      })
                      .describe(
                        "Reference to the `Secret` which holds the certificate and private key pair which will be used for this listener. The certificate can optionally contain the whole chain. This field can be used only with listeners with enabled TLS encryption."
                      )
                      .optional(),
                    externalTrafficPolicy: z
                      .enum(["Local", "Cluster"])
                      .describe(
                        "Specifies whether the service routes external traffic to node-local or cluster-wide endpoints. `Cluster` may cause a second hop to another node and obscures the client source IP. `Local` avoids a second hop for LoadBalancer and Nodeport type services and preserves the client source IP (when supported by the infrastructure). If unspecified, Kubernetes will use `Cluster` as the default.This field can be used only with `loadbalancer` or `nodeport` type listener."
                      )
                      .optional(),
                    loadBalancerSourceRanges: z
                      .array(z.string())
                      .describe(
                        "A list of CIDR ranges (for example `10.0.0.0/8` or `130.211.204.1/32`) from which clients can connect to load balancer type listeners. If supported by the platform, traffic through the loadbalancer is restricted to the specified CIDR ranges. This field is applicable only for loadbalancer type services and is ignored if the cloud provider does not support the feature. This field can be used only with `loadbalancer` type listener."
                      )
                      .optional(),
                    bootstrap: z
                      .object({
                        alternativeNames: z
                          .array(z.string())
                          .describe(
                            "Additional alternative names for the bootstrap service. The alternative names will be added to the list of subject alternative names of the TLS certificates."
                          )
                          .optional(),
                        host: z
                          .string()
                          .describe(
                            "The bootstrap host. This field will be used in the Ingress resource or in the Route resource to specify the desired hostname. This field can be used only with `route` (optional) or `ingress` (required) type listeners."
                          )
                          .optional(),
                        nodePort: z
                          .number()
                          .int()
                          .describe(
                            "Node port for the bootstrap service. This field can be used only with `nodeport` type listener."
                          )
                          .optional(),
                        loadBalancerIP: z
                          .string()
                          .describe(
                            "The loadbalancer is requested with the IP address specified in this field. This feature depends on whether the underlying cloud provider supports specifying the `loadBalancerIP` when a load balancer is created. This field is ignored if the cloud provider does not support the feature.This field can be used only with `loadbalancer` type listener."
                          )
                          .optional(),
                        annotations: z
                          .record(z.any())
                          .describe(
                            "Annotations that will be added to the `Ingress`, `Route`, or `Service` resource. You can use this field to configure DNS providers such as External DNS. This field can be used only with `loadbalancer`, `nodeport`, `route`, or `ingress` type listeners."
                          )
                          .optional(),
                        labels: z
                          .record(z.any())
                          .describe(
                            "Labels that will be added to the `Ingress`, `Route`, or `Service` resource. This field can be used only with `loadbalancer`, `nodeport`, `route`, or `ingress` type listeners."
                          )
                          .optional(),
                      })
                      .describe("Bootstrap configuration.")
                      .optional(),
                    brokers: z
                      .array(
                        z.object({
                          broker: z
                            .number()
                            .int()
                            .describe(
                              "ID of the kafka broker (broker identifier). Broker IDs start from 0 and correspond to the number of broker replicas."
                            ),
                          advertisedHost: z
                            .string()
                            .describe(
                              "The host name which will be used in the brokers' `advertised.brokers`."
                            )
                            .optional(),
                          advertisedPort: z
                            .number()
                            .int()
                            .describe(
                              "The port number which will be used in the brokers' `advertised.brokers`."
                            )
                            .optional(),
                          host: z
                            .string()
                            .describe(
                              "The broker host. This field will be used in the Ingress resource or in the Route resource to specify the desired hostname. This field can be used only with `route` (optional) or `ingress` (required) type listeners."
                            )
                            .optional(),
                          nodePort: z
                            .number()
                            .int()
                            .describe(
                              "Node port for the per-broker service. This field can be used only with `nodeport` type listener."
                            )
                            .optional(),
                          loadBalancerIP: z
                            .string()
                            .describe(
                              "The loadbalancer is requested with the IP address specified in this field. This feature depends on whether the underlying cloud provider supports specifying the `loadBalancerIP` when a load balancer is created. This field is ignored if the cloud provider does not support the feature.This field can be used only with `loadbalancer` type listener."
                            )
                            .optional(),
                          annotations: z
                            .record(z.any())
                            .describe(
                              "Annotations that will be added to the `Ingress` or `Service` resource. You can use this field to configure DNS providers such as External DNS. This field can be used only with `loadbalancer`, `nodeport`, or `ingress` type listeners."
                            )
                            .optional(),
                          labels: z
                            .record(z.any())
                            .describe(
                              "Labels that will be added to the `Ingress`, `Route`, or `Service` resource. This field can be used only with `loadbalancer`, `nodeport`, `route`, or `ingress` type listeners."
                            )
                            .optional(),
                        })
                      )
                      .describe("Per-broker configurations.")
                      .optional(),
                    ipFamilyPolicy: z
                      .enum([
                        "SingleStack",
                        "PreferDualStack",
                        "RequireDualStack",
                      ])
                      .describe(
                        "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                      )
                      .optional(),
                    ipFamilies: z
                      .array(z.enum(["IPv4", "IPv6"]))
                      .describe(
                        "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                      )
                      .optional(),
                    createBootstrapService: z
                      .boolean()
                      .describe(
                        "Whether to create the bootstrap service or not. The bootstrap service is created by default (if not specified differently). This field can be used with the `loadBalancer` type listener."
                      )
                      .optional(),
                    class: z
                      .string()
                      .describe(
                        "Configures a specific class for `Ingress` and `LoadBalancer` that defines which controller will be used. This field can only be used with `ingress` and `loadbalancer` type listeners. If not specified, the default controller is used. For an `ingress` listener, set the `ingressClassName` property in the `Ingress` resources. For a `loadbalancer` listener, set the `loadBalancerClass` property  in the `Service` resources."
                      )
                      .optional(),
                    finalizers: z
                      .array(z.string())
                      .describe(
                        "A list of finalizers which will be configured for the `LoadBalancer` type Services created for this listener. If supported by the platform, the finalizer `service.kubernetes.io/load-balancer-cleanup` to make sure that the external load balancer is deleted together with the service.For more information, see https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/#garbage-collecting-load-balancers. This field can be used only with `loadbalancer` type listeners."
                      )
                      .optional(),
                    maxConnectionCreationRate: z
                      .number()
                      .int()
                      .describe(
                        "The maximum connection creation rate we allow in this listener at any time. New connections will be throttled if the limit is reached."
                      )
                      .optional(),
                    maxConnections: z
                      .number()
                      .int()
                      .describe(
                        "The maximum number of connections we allow for this listener in the broker at any time. New connections are blocked if the limit is reached."
                      )
                      .optional(),
                    preferredNodePortAddressType: z
                      .enum([
                        "ExternalIP",
                        "ExternalDNS",
                        "InternalIP",
                        "InternalDNS",
                        "Hostname",
                      ])
                      .describe(
                        "Defines which address type should be used as the node address. Available types are: `ExternalDNS`, `ExternalIP`, `InternalDNS`, `InternalIP` and `Hostname`. By default, the addresses will be used in the following order (the first one found will be used):\n\n* `ExternalDNS`\n* `ExternalIP`\n* `InternalDNS`\n* `InternalIP`\n* `Hostname`\n\nThis field is used to select the preferred address type, which is checked first. If no address is found for this address type, the other types are checked in the default order. This field can only be used with `nodeport` type listener."
                      )
                      .optional(),
                    useServiceDnsDomain: z
                      .boolean()
                      .describe(
                        "Configures whether the Kubernetes service DNS domain should be used or not. If set to `true`, the generated addresses will contain the service DNS domain suffix (by default `.cluster.local`, can be configured using environment variable `KUBERNETES_SERVICE_DNS_DOMAIN`). Defaults to `false`.This field can be used only with `internal` and `cluster-ip` type listeners."
                      )
                      .optional(),
                  })
                  .describe("Additional listener configuration.")
                  .optional(),
                networkPolicyPeers: z
                  .array(
                    z.object({
                      ipBlock: z
                        .object({
                          cidr: z.string().optional(),
                          except: z.array(z.string()).optional(),
                        })
                        .optional(),
                      namespaceSelector: z
                        .object({
                          matchExpressions: z
                            .array(
                              z.object({
                                key: z.string().optional(),
                                operator: z.string().optional(),
                                values: z.array(z.string()).optional(),
                              })
                            )
                            .optional(),
                          matchLabels: z.record(z.any()).optional(),
                        })
                        .optional(),
                      podSelector: z
                        .object({
                          matchExpressions: z
                            .array(
                              z.object({
                                key: z.string().optional(),
                                operator: z.string().optional(),
                                values: z.array(z.string()).optional(),
                              })
                            )
                            .optional(),
                          matchLabels: z.record(z.any()).optional(),
                        })
                        .optional(),
                    })
                  )
                  .describe(
                    "List of peers which should be able to connect to this listener. Peers in this list are combined using a logical OR operation. If this field is empty or missing, all connections will be allowed for this listener. If this field is present and contains at least one item, the listener only allows the traffic which matches at least one item in this list."
                  )
                  .optional(),
              })
            )
            .min(1)
            .describe("Configures listeners of Kafka brokers."),
          config: z
            .record(z.any())
            .describe(
              "Kafka broker config properties with the following prefixes cannot be set: listeners, advertised., broker., listener., host.name, port, inter.broker.listener.name, sasl., ssl., security., password., log.dir, zookeeper.connect, zookeeper.set.acl, zookeeper.ssl, zookeeper.clientCnxnSocket, authorizer., super.user, cruise.control.metrics.topic, cruise.control.metrics.reporter.bootstrap.servers,node.id, process.roles, controller. (with the exception of: zookeeper.connection.timeout.ms, sasl.server.max.receive.size,ssl.cipher.suites, ssl.protocol, ssl.enabled.protocols, ssl.secure.random.implementation,cruise.control.metrics.topic.num.partitions, cruise.control.metrics.topic.replication.factor, cruise.control.metrics.topic.retention.ms,cruise.control.metrics.topic.auto.create.retries, cruise.control.metrics.topic.auto.create.timeout.ms,cruise.control.metrics.topic.min.insync.replicas,controller.quorum.election.backoff.max.ms, controller.quorum.election.timeout.ms, controller.quorum.fetch.timeout.ms)."
            )
            .optional(),
          storage: z
            .object({
              class: z
                .string()
                .describe(
                  "The storage class to use for dynamic volume allocation."
                )
                .optional(),
              deleteClaim: z
                .boolean()
                .describe(
                  "Specifies if the persistent volume claim has to be deleted when the cluster is un-deployed."
                )
                .optional(),
              id: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "Storage identification number. It is mandatory only for storage volumes defined in a storage of type 'jbod'."
                )
                .optional(),
              overrides: z
                .array(
                  z.object({
                    class: z
                      .string()
                      .describe(
                        "The storage class to use for dynamic volume allocation for this broker."
                      )
                      .optional(),
                    broker: z
                      .number()
                      .int()
                      .describe("Id of the kafka broker (broker identifier).")
                      .optional(),
                  })
                )
                .describe(
                  "Overrides for individual brokers. The `overrides` field allows to specify a different configuration for different brokers."
                )
                .optional(),
              selector: z
                .record(z.any())
                .describe(
                  "Specifies a specific persistent volume to use. It contains key:value pairs representing labels for selecting such a volume."
                )
                .optional(),
              size: z
                .string()
                .describe(
                  "When type=persistent-claim, defines the size of the persistent volume claim (i.e 1Gi). Mandatory when type=persistent-claim."
                )
                .optional(),
              sizeLimit: z
                .string()
                .regex(new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$"))
                .describe(
                  "When type=ephemeral, defines the total amount of local storage required for this EmptyDir volume (for example 1Gi)."
                )
                .optional(),
              type: z
                .enum(["ephemeral", "persistent-claim", "jbod"])
                .describe(
                  "Storage type, must be either 'ephemeral', 'persistent-claim', or 'jbod'."
                ),
              volumes: z
                .array(
                  z.object({
                    class: z
                      .string()
                      .describe(
                        "The storage class to use for dynamic volume allocation."
                      )
                      .optional(),
                    deleteClaim: z
                      .boolean()
                      .describe(
                        "Specifies if the persistent volume claim has to be deleted when the cluster is un-deployed."
                      )
                      .optional(),
                    id: z
                      .number()
                      .int()
                      .gte(0)
                      .describe(
                        "Storage identification number. It is mandatory only for storage volumes defined in a storage of type 'jbod'."
                      )
                      .optional(),
                    overrides: z
                      .array(
                        z.object({
                          class: z
                            .string()
                            .describe(
                              "The storage class to use for dynamic volume allocation for this broker."
                            )
                            .optional(),
                          broker: z
                            .number()
                            .int()
                            .describe(
                              "Id of the kafka broker (broker identifier)."
                            )
                            .optional(),
                        })
                      )
                      .describe(
                        "Overrides for individual brokers. The `overrides` field allows to specify a different configuration for different brokers."
                      )
                      .optional(),
                    selector: z
                      .record(z.any())
                      .describe(
                        "Specifies a specific persistent volume to use. It contains key:value pairs representing labels for selecting such a volume."
                      )
                      .optional(),
                    size: z
                      .string()
                      .describe(
                        "When type=persistent-claim, defines the size of the persistent volume claim (i.e 1Gi). Mandatory when type=persistent-claim."
                      )
                      .optional(),
                    sizeLimit: z
                      .string()
                      .regex(
                        new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                      )
                      .describe(
                        "When type=ephemeral, defines the total amount of local storage required for this EmptyDir volume (for example 1Gi)."
                      )
                      .optional(),
                    type: z
                      .enum(["ephemeral", "persistent-claim"])
                      .describe(
                        "Storage type, must be either 'ephemeral' or 'persistent-claim'."
                      ),
                  })
                )
                .describe(
                  "List of volumes as Storage objects representing the JBOD disks array."
                )
                .optional(),
            })
            .describe("Storage configuration (disk). Cannot be updated."),
          authorization: z
            .object({
              allowOnError: z
                .boolean()
                .describe(
                  "Defines whether a Kafka client should be allowed or denied by default when the authorizer fails to query the Open Policy Agent, for example, when it is temporarily unavailable). Defaults to `false` - all actions will be denied."
                )
                .optional(),
              authorizerClass: z
                .string()
                .describe(
                  "Authorization implementation class, which must be available in classpath."
                )
                .optional(),
              clientId: z
                .string()
                .describe(
                  "OAuth Client ID which the Kafka client can use to authenticate against the OAuth server and use the token endpoint URI."
                )
                .optional(),
              connectTimeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The connect timeout in seconds when connecting to authorization server. If not set, the effective connect timeout is 60 seconds."
                )
                .optional(),
              delegateToKafkaAcls: z
                .boolean()
                .describe(
                  "Whether authorization decision should be delegated to the 'Simple' authorizer if DENIED by Keycloak Authorization Services policies. Default value is `false`."
                )
                .optional(),
              disableTlsHostnameVerification: z
                .boolean()
                .describe(
                  "Enable or disable TLS hostname verification. Default value is `false`."
                )
                .optional(),
              enableMetrics: z
                .boolean()
                .describe(
                  "Enable or disable OAuth metrics. Default value is `false`."
                )
                .optional(),
              expireAfterMs: z
                .number()
                .int()
                .describe(
                  "The expiration of the records kept in the local cache to avoid querying the Open Policy Agent for every request. Defines how often the cached authorization decisions are reloaded from the Open Policy Agent server. In milliseconds. Defaults to `3600000`."
                )
                .optional(),
              grantsRefreshPeriodSeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The time between two consecutive grants refresh runs in seconds. The default value is 60."
                )
                .optional(),
              grantsRefreshPoolSize: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The number of threads to use to refresh grants for active sessions. The more threads, the more parallelism, so the sooner the job completes. However, using more threads places a heavier load on the authorization server. The default value is 5."
                )
                .optional(),
              httpRetries: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The maximum number of retries to attempt if an initial HTTP request fails. If not set, the default is to not attempt any retries."
                )
                .optional(),
              initialCacheCapacity: z
                .number()
                .int()
                .describe(
                  "Initial capacity of the local cache used by the authorizer to avoid querying the Open Policy Agent for every request Defaults to `5000`."
                )
                .optional(),
              maximumCacheSize: z
                .number()
                .int()
                .describe(
                  "Maximum capacity of the local cache used by the authorizer to avoid querying the Open Policy Agent for every request. Defaults to `50000`."
                )
                .optional(),
              readTimeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The read timeout in seconds when connecting to authorization server. If not set, the effective read timeout is 60 seconds."
                )
                .optional(),
              superUsers: z
                .array(z.string())
                .describe(
                  "List of super users, which are user principals with unlimited access rights."
                )
                .optional(),
              supportsAdminApi: z
                .boolean()
                .describe(
                  "Indicates whether the custom authorizer supports the APIs for managing ACLs using the Kafka Admin API. Defaults to `false`."
                )
                .optional(),
              tlsTrustedCertificates: z
                .array(
                  z.object({
                    certificate: z
                      .string()
                      .describe(
                        "The name of the file certificate in the Secret."
                      ),
                    secretName: z
                      .string()
                      .describe(
                        "The name of the Secret containing the certificate."
                      ),
                  })
                )
                .describe(
                  "Trusted certificates for TLS connection to the OAuth server."
                )
                .optional(),
              tokenEndpointUri: z
                .string()
                .describe("Authorization server token endpoint URI.")
                .optional(),
              type: z
                .enum(["simple", "opa", "keycloak", "custom"])
                .describe(
                  "Authorization type. Currently, the supported types are `simple`, `keycloak`, `opa` and `custom`. `simple` authorization type uses Kafka's `kafka.security.authorizer.AclAuthorizer` class for authorization. `keycloak` authorization type uses Keycloak Authorization Services for authorization. `opa` authorization type uses Open Policy Agent based authorization.`custom` authorization type uses user-provided implementation for authorization."
                ),
              url: z
                .string()
                .describe(
                  "The URL used to connect to the Open Policy Agent server. The URL has to include the policy which will be queried by the authorizer. This option is required."
                )
                .optional(),
            })
            .describe("Authorization configuration for Kafka brokers.")
            .optional(),
          rack: z
            .object({
              topologyKey: z
                .string()
                .describe(
                  "A key that matches labels assigned to the Kubernetes cluster nodes. The value of the label is used to set a broker's `broker.rack` config, and the `client.rack` config for Kafka Connect or MirrorMaker 2."
                ),
            })
            .describe("Configuration of the `broker.rack` broker config.")
            .optional(),
          brokerRackInitImage: z
            .string()
            .describe(
              "The image of the init container used for initializing the `broker.rack`."
            )
            .optional(),
          livenessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod liveness checking.")
            .optional(),
          readinessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod readiness checking.")
            .optional(),
          jvmOptions: z
            .object({
              "-XX": z
                .record(z.any())
                .describe("A map of -XX options to the JVM.")
                .optional(),
              "-Xms": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xms option to to the JVM.")
                .optional(),
              "-Xmx": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xmx option to to the JVM.")
                .optional(),
              gcLoggingEnabled: z
                .boolean()
                .describe(
                  "Specifies whether the Garbage Collection logging is enabled. The default is false."
                )
                .optional(),
              javaSystemProperties: z
                .array(
                  z.object({
                    name: z
                      .string()
                      .describe("The system property name.")
                      .optional(),
                    value: z
                      .string()
                      .describe("The system property value.")
                      .optional(),
                  })
                )
                .describe(
                  "A map of additional system properties which will be passed using the `-D` option to the JVM."
                )
                .optional(),
            })
            .describe("JVM Options for pods.")
            .optional(),
          jmxOptions: z
            .object({
              authentication: z
                .object({
                  type: z
                    .enum(["password"])
                    .describe(
                      "Authentication type. Currently the only supported types are `password`.`password` type creates a username and protected port with no TLS."
                    ),
                })
                .describe(
                  "Authentication configuration for connecting to the JMX port."
                )
                .optional(),
            })
            .describe("JMX Options for Kafka brokers.")
            .optional(),
          resources: z
            .object({
              claims: z
                .array(z.object({ name: z.string().optional() }))
                .optional(),
              limits: z.record(z.any()).optional(),
              requests: z.record(z.any()).optional(),
            })
            .describe("CPU and memory resources to reserve.")
            .optional(),
          metricsConfig: z
            .object({
              type: z
                .enum(["jmxPrometheusExporter"])
                .describe(
                  "Metrics type. Only 'jmxPrometheusExporter' supported currently."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "ConfigMap entry where the Prometheus JMX Exporter configuration is stored. For details of the structure of this configuration, see the {JMXExporter}."
                ),
            })
            .describe("Metrics configuration.")
            .optional(),
          logging: z
            .object({
              loggers: z
                .record(z.any())
                .describe("A Map from logger name to logger level.")
                .optional(),
              type: z
                .enum(["inline", "external"])
                .describe(
                  "Logging type, must be either 'inline' or 'external'."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "`ConfigMap` entry where the logging configuration is stored. "
                )
                .optional(),
            })
            .describe("Logging configuration for Kafka.")
            .optional(),
          template: z
            .object({
              statefulset: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  podManagementPolicy: z
                    .enum(["OrderedReady", "Parallel"])
                    .describe(
                      "PodManagementPolicy which will be used for this StatefulSet. Valid values are `Parallel` and `OrderedReady`. Defaults to `Parallel`."
                    )
                    .optional(),
                })
                .describe("Template for Kafka `StatefulSet`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for Kafka `Pods`.")
                .optional(),
              bootstrapService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  ipFamilyPolicy: z
                    .enum([
                      "SingleStack",
                      "PreferDualStack",
                      "RequireDualStack",
                    ])
                    .describe(
                      "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                  ipFamilies: z
                    .array(z.enum(["IPv4", "IPv6"]))
                    .describe(
                      "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                })
                .describe("Template for Kafka bootstrap `Service`.")
                .optional(),
              brokersService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  ipFamilyPolicy: z
                    .enum([
                      "SingleStack",
                      "PreferDualStack",
                      "RequireDualStack",
                    ])
                    .describe(
                      "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                  ipFamilies: z
                    .array(z.enum(["IPv4", "IPv6"]))
                    .describe(
                      "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                })
                .describe("Template for Kafka broker `Service`.")
                .optional(),
              externalBootstrapService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for Kafka external bootstrap `Service`.")
                .optional(),
              perPodService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Kafka per-pod `Services` used for access from outside of Kubernetes."
                )
                .optional(),
              externalBootstrapRoute: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for Kafka external bootstrap `Route`.")
                .optional(),
              perPodRoute: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Kafka per-pod `Routes` used for access from outside of OpenShift."
                )
                .optional(),
              externalBootstrapIngress: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for Kafka external bootstrap `Ingress`.")
                .optional(),
              perPodIngress: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Kafka per-pod `Ingress` used for access from outside of Kubernetes."
                )
                .optional(),
              persistentVolumeClaim: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for all Kafka `PersistentVolumeClaims`.")
                .optional(),
              podDisruptionBudget: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe(
                      "Metadata to apply to the `PodDisruptionBudgetTemplate` resource."
                    )
                    .optional(),
                  maxUnavailable: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "Maximum number of unavailable pods to allow automatic Pod eviction. A Pod eviction is allowed when the `maxUnavailable` number of pods or fewer are unavailable after the eviction. Setting this value to 0 prevents all voluntary evictions, so the pods must be evicted manually. Defaults to 1."
                    )
                    .optional(),
                })
                .describe("Template for Kafka `PodDisruptionBudget`.")
                .optional(),
              kafkaContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Kafka broker container.")
                .optional(),
              initContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Kafka init container.")
                .optional(),
              clusterCaCert: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Secret with Kafka Cluster certificate public key."
                )
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Kafka service account.")
                .optional(),
              jmxSecret: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Secret of the Kafka Cluster JMX authentication."
                )
                .optional(),
              clusterRoleBinding: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Kafka ClusterRoleBinding.")
                .optional(),
              podSet: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for Kafka `StrimziPodSet` resource.")
                .optional(),
            })
            .describe(
              "Template for Kafka cluster resources. The template allows users to specify how the Kubernetes resources are generated."
            )
            .optional(),
        })
        .describe("Configuration of the Kafka cluster."),
      zookeeper: z
        .object({
          replicas: z
            .number()
            .int()
            .gte(1)
            .describe("The number of pods in the cluster."),
          image: z
            .string()
            .describe("The docker image for the pods.")
            .optional(),
          storage: z
            .object({
              class: z
                .string()
                .describe(
                  "The storage class to use for dynamic volume allocation."
                )
                .optional(),
              deleteClaim: z
                .boolean()
                .describe(
                  "Specifies if the persistent volume claim has to be deleted when the cluster is un-deployed."
                )
                .optional(),
              id: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "Storage identification number. It is mandatory only for storage volumes defined in a storage of type 'jbod'."
                )
                .optional(),
              overrides: z
                .array(
                  z.object({
                    class: z
                      .string()
                      .describe(
                        "The storage class to use for dynamic volume allocation for this broker."
                      )
                      .optional(),
                    broker: z
                      .number()
                      .int()
                      .describe("Id of the kafka broker (broker identifier).")
                      .optional(),
                  })
                )
                .describe(
                  "Overrides for individual brokers. The `overrides` field allows to specify a different configuration for different brokers."
                )
                .optional(),
              selector: z
                .record(z.any())
                .describe(
                  "Specifies a specific persistent volume to use. It contains key:value pairs representing labels for selecting such a volume."
                )
                .optional(),
              size: z
                .string()
                .describe(
                  "When type=persistent-claim, defines the size of the persistent volume claim (i.e 1Gi). Mandatory when type=persistent-claim."
                )
                .optional(),
              sizeLimit: z
                .string()
                .regex(new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$"))
                .describe(
                  "When type=ephemeral, defines the total amount of local storage required for this EmptyDir volume (for example 1Gi)."
                )
                .optional(),
              type: z
                .enum(["ephemeral", "persistent-claim"])
                .describe(
                  "Storage type, must be either 'ephemeral' or 'persistent-claim'."
                ),
            })
            .describe("Storage configuration (disk). Cannot be updated."),
          config: z
            .record(z.any())
            .describe(
              "The ZooKeeper broker config. Properties with the following prefixes cannot be set: server., dataDir, dataLogDir, clientPort, authProvider, quorum.auth, requireClientAuthScheme, snapshot.trust.empty, standaloneEnabled, reconfigEnabled, 4lw.commands.whitelist, secureClientPort, ssl., serverCnxnFactory, sslQuorum (with the exception of: ssl.protocol, ssl.quorum.protocol, ssl.enabledProtocols, ssl.quorum.enabledProtocols, ssl.ciphersuites, ssl.quorum.ciphersuites, ssl.hostnameVerification, ssl.quorum.hostnameVerification)."
            )
            .optional(),
          livenessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod liveness checking.")
            .optional(),
          readinessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod readiness checking.")
            .optional(),
          jvmOptions: z
            .object({
              "-XX": z
                .record(z.any())
                .describe("A map of -XX options to the JVM.")
                .optional(),
              "-Xms": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xms option to to the JVM.")
                .optional(),
              "-Xmx": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xmx option to to the JVM.")
                .optional(),
              gcLoggingEnabled: z
                .boolean()
                .describe(
                  "Specifies whether the Garbage Collection logging is enabled. The default is false."
                )
                .optional(),
              javaSystemProperties: z
                .array(
                  z.object({
                    name: z
                      .string()
                      .describe("The system property name.")
                      .optional(),
                    value: z
                      .string()
                      .describe("The system property value.")
                      .optional(),
                  })
                )
                .describe(
                  "A map of additional system properties which will be passed using the `-D` option to the JVM."
                )
                .optional(),
            })
            .describe("JVM Options for pods.")
            .optional(),
          jmxOptions: z
            .object({
              authentication: z
                .object({
                  type: z
                    .enum(["password"])
                    .describe(
                      "Authentication type. Currently the only supported types are `password`.`password` type creates a username and protected port with no TLS."
                    ),
                })
                .describe(
                  "Authentication configuration for connecting to the JMX port."
                )
                .optional(),
            })
            .describe("JMX Options for Zookeeper nodes.")
            .optional(),
          resources: z
            .object({
              claims: z
                .array(z.object({ name: z.string().optional() }))
                .optional(),
              limits: z.record(z.any()).optional(),
              requests: z.record(z.any()).optional(),
            })
            .describe("CPU and memory resources to reserve.")
            .optional(),
          metricsConfig: z
            .object({
              type: z
                .enum(["jmxPrometheusExporter"])
                .describe(
                  "Metrics type. Only 'jmxPrometheusExporter' supported currently."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "ConfigMap entry where the Prometheus JMX Exporter configuration is stored. For details of the structure of this configuration, see the {JMXExporter}."
                ),
            })
            .describe("Metrics configuration.")
            .optional(),
          logging: z
            .object({
              loggers: z
                .record(z.any())
                .describe("A Map from logger name to logger level.")
                .optional(),
              type: z
                .enum(["inline", "external"])
                .describe(
                  "Logging type, must be either 'inline' or 'external'."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "`ConfigMap` entry where the logging configuration is stored. "
                )
                .optional(),
            })
            .describe("Logging configuration for ZooKeeper.")
            .optional(),
          template: z
            .object({
              statefulset: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  podManagementPolicy: z
                    .enum(["OrderedReady", "Parallel"])
                    .describe(
                      "PodManagementPolicy which will be used for this StatefulSet. Valid values are `Parallel` and `OrderedReady`. Defaults to `Parallel`."
                    )
                    .optional(),
                })
                .describe("Template for ZooKeeper `StatefulSet`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for ZooKeeper `Pods`.")
                .optional(),
              clientService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  ipFamilyPolicy: z
                    .enum([
                      "SingleStack",
                      "PreferDualStack",
                      "RequireDualStack",
                    ])
                    .describe(
                      "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                  ipFamilies: z
                    .array(z.enum(["IPv4", "IPv6"]))
                    .describe(
                      "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                })
                .describe("Template for ZooKeeper client `Service`.")
                .optional(),
              nodesService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  ipFamilyPolicy: z
                    .enum([
                      "SingleStack",
                      "PreferDualStack",
                      "RequireDualStack",
                    ])
                    .describe(
                      "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                  ipFamilies: z
                    .array(z.enum(["IPv4", "IPv6"]))
                    .describe(
                      "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                })
                .describe("Template for ZooKeeper nodes `Service`.")
                .optional(),
              persistentVolumeClaim: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for all ZooKeeper `PersistentVolumeClaims`."
                )
                .optional(),
              podDisruptionBudget: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe(
                      "Metadata to apply to the `PodDisruptionBudgetTemplate` resource."
                    )
                    .optional(),
                  maxUnavailable: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "Maximum number of unavailable pods to allow automatic Pod eviction. A Pod eviction is allowed when the `maxUnavailable` number of pods or fewer are unavailable after the eviction. Setting this value to 0 prevents all voluntary evictions, so the pods must be evicted manually. Defaults to 1."
                    )
                    .optional(),
                })
                .describe("Template for ZooKeeper `PodDisruptionBudget`.")
                .optional(),
              zookeeperContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the ZooKeeper container.")
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the ZooKeeper service account.")
                .optional(),
              jmxSecret: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe(
                  "Template for Secret of the Zookeeper Cluster JMX authentication."
                )
                .optional(),
              podSet: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for ZooKeeper `StrimziPodSet` resource.")
                .optional(),
            })
            .describe(
              "Template for ZooKeeper cluster resources. The template allows users to specify how the Kubernetes resources are generated."
            )
            .optional(),
        })
        .describe("Configuration of the ZooKeeper cluster."),
      entityOperator: z
        .object({
          topicOperator: z
            .object({
              watchedNamespace: z
                .string()
                .describe("The namespace the Topic Operator should watch.")
                .optional(),
              image: z
                .string()
                .describe("The image to use for the Topic Operator.")
                .optional(),
              reconciliationIntervalSeconds: z
                .number()
                .int()
                .gte(0)
                .describe("Interval between periodic reconciliations.")
                .optional(),
              zookeeperSessionTimeoutSeconds: z
                .number()
                .int()
                .gte(0)
                .describe("Timeout for the ZooKeeper session.")
                .optional(),
              startupProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod startup checking.")
                .optional(),
              livenessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod liveness checking.")
                .optional(),
              readinessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod readiness checking.")
                .optional(),
              resources: z
                .object({
                  claims: z
                    .array(z.object({ name: z.string().optional() }))
                    .optional(),
                  limits: z.record(z.any()).optional(),
                  requests: z.record(z.any()).optional(),
                })
                .describe("CPU and memory resources to reserve.")
                .optional(),
              topicMetadataMaxAttempts: z
                .number()
                .int()
                .gte(0)
                .describe("The number of attempts at getting topic metadata.")
                .optional(),
              logging: z
                .object({
                  loggers: z
                    .record(z.any())
                    .describe("A Map from logger name to logger level.")
                    .optional(),
                  type: z
                    .enum(["inline", "external"])
                    .describe(
                      "Logging type, must be either 'inline' or 'external'."
                    ),
                  valueFrom: z
                    .object({
                      configMapKeyRef: z
                        .object({
                          key: z.string().optional(),
                          name: z.string().optional(),
                          optional: z.boolean().optional(),
                        })
                        .describe(
                          "Reference to the key in the ConfigMap containing the configuration."
                        )
                        .optional(),
                    })
                    .describe(
                      "`ConfigMap` entry where the logging configuration is stored. "
                    )
                    .optional(),
                })
                .describe("Logging configuration.")
                .optional(),
              jvmOptions: z
                .object({
                  "-XX": z
                    .record(z.any())
                    .describe("A map of -XX options to the JVM.")
                    .optional(),
                  "-Xms": z
                    .string()
                    .regex(new RegExp("^[0-9]+[mMgG]?$"))
                    .describe("-Xms option to to the JVM.")
                    .optional(),
                  "-Xmx": z
                    .string()
                    .regex(new RegExp("^[0-9]+[mMgG]?$"))
                    .describe("-Xmx option to to the JVM.")
                    .optional(),
                  gcLoggingEnabled: z
                    .boolean()
                    .describe(
                      "Specifies whether the Garbage Collection logging is enabled. The default is false."
                    )
                    .optional(),
                  javaSystemProperties: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The system property name.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The system property value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "A map of additional system properties which will be passed using the `-D` option to the JVM."
                    )
                    .optional(),
                })
                .describe("JVM Options for pods.")
                .optional(),
            })
            .describe("Configuration of the Topic Operator.")
            .optional(),
          userOperator: z
            .object({
              watchedNamespace: z
                .string()
                .describe("The namespace the User Operator should watch.")
                .optional(),
              image: z
                .string()
                .describe("The image to use for the User Operator.")
                .optional(),
              reconciliationIntervalSeconds: z
                .number()
                .int()
                .gte(0)
                .describe("Interval between periodic reconciliations.")
                .optional(),
              zookeeperSessionTimeoutSeconds: z
                .number()
                .int()
                .gte(0)
                .describe("Timeout for the ZooKeeper session.")
                .optional(),
              secretPrefix: z
                .string()
                .describe(
                  "The prefix that will be added to the KafkaUser name to be used as the Secret name."
                )
                .optional(),
              livenessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod liveness checking.")
                .optional(),
              readinessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod readiness checking.")
                .optional(),
              resources: z
                .object({
                  claims: z
                    .array(z.object({ name: z.string().optional() }))
                    .optional(),
                  limits: z.record(z.any()).optional(),
                  requests: z.record(z.any()).optional(),
                })
                .describe("CPU and memory resources to reserve.")
                .optional(),
              logging: z
                .object({
                  loggers: z
                    .record(z.any())
                    .describe("A Map from logger name to logger level.")
                    .optional(),
                  type: z
                    .enum(["inline", "external"])
                    .describe(
                      "Logging type, must be either 'inline' or 'external'."
                    ),
                  valueFrom: z
                    .object({
                      configMapKeyRef: z
                        .object({
                          key: z.string().optional(),
                          name: z.string().optional(),
                          optional: z.boolean().optional(),
                        })
                        .describe(
                          "Reference to the key in the ConfigMap containing the configuration."
                        )
                        .optional(),
                    })
                    .describe(
                      "`ConfigMap` entry where the logging configuration is stored. "
                    )
                    .optional(),
                })
                .describe("Logging configuration.")
                .optional(),
              jvmOptions: z
                .object({
                  "-XX": z
                    .record(z.any())
                    .describe("A map of -XX options to the JVM.")
                    .optional(),
                  "-Xms": z
                    .string()
                    .regex(new RegExp("^[0-9]+[mMgG]?$"))
                    .describe("-Xms option to to the JVM.")
                    .optional(),
                  "-Xmx": z
                    .string()
                    .regex(new RegExp("^[0-9]+[mMgG]?$"))
                    .describe("-Xmx option to to the JVM.")
                    .optional(),
                  gcLoggingEnabled: z
                    .boolean()
                    .describe(
                      "Specifies whether the Garbage Collection logging is enabled. The default is false."
                    )
                    .optional(),
                  javaSystemProperties: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The system property name.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The system property value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "A map of additional system properties which will be passed using the `-D` option to the JVM."
                    )
                    .optional(),
                })
                .describe("JVM Options for pods.")
                .optional(),
            })
            .describe("Configuration of the User Operator.")
            .optional(),
          tlsSidecar: z
            .object({
              image: z
                .string()
                .describe("The docker image for the container.")
                .optional(),
              livenessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod liveness checking.")
                .optional(),
              logLevel: z
                .enum([
                  "emerg",
                  "alert",
                  "crit",
                  "err",
                  "warning",
                  "notice",
                  "info",
                  "debug",
                ])
                .describe(
                  "The log level for the TLS sidecar. Default value is `notice`."
                )
                .optional(),
              readinessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod readiness checking.")
                .optional(),
              resources: z
                .object({
                  claims: z
                    .array(z.object({ name: z.string().optional() }))
                    .optional(),
                  limits: z.record(z.any()).optional(),
                  requests: z.record(z.any()).optional(),
                })
                .describe("CPU and memory resources to reserve.")
                .optional(),
            })
            .describe("TLS sidecar configuration.")
            .optional(),
          template: z
            .object({
              deployment: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  deploymentStrategy: z
                    .enum(["RollingUpdate", "Recreate"])
                    .describe(
                      "Pod replacement strategy for deployment configuration changes. Valid values are `RollingUpdate` and `Recreate`. Defaults to `RollingUpdate`."
                    )
                    .optional(),
                })
                .describe("Template for Entity Operator `Deployment`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for Entity Operator `Pods`.")
                .optional(),
              topicOperatorContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Entity Topic Operator container.")
                .optional(),
              userOperatorContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Entity User Operator container.")
                .optional(),
              tlsSidecarContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe(
                  "Template for the Entity Operator TLS sidecar container."
                )
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Entity Operator service account.")
                .optional(),
              entityOperatorRole: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Entity Operator Role.")
                .optional(),
              topicOperatorRoleBinding: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Entity Topic Operator RoleBinding.")
                .optional(),
              userOperatorRoleBinding: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Entity Topic Operator RoleBinding.")
                .optional(),
            })
            .describe(
              "Template for Entity Operator resources. The template allows users to specify how a `Deployment` and `Pod` is generated."
            )
            .optional(),
        })
        .describe("Configuration of the Entity Operator.")
        .optional(),
      clusterCa: z
        .object({
          generateCertificateAuthority: z
            .boolean()
            .describe(
              "If true then Certificate Authority certificates will be generated automatically. Otherwise the user will need to provide a Secret with the CA certificate. Default is true."
            )
            .optional(),
          generateSecretOwnerReference: z
            .boolean()
            .describe(
              "If `true`, the Cluster and Client CA Secrets are configured with the `ownerReference` set to the `Kafka` resource. If the `Kafka` resource is deleted when `true`, the CA Secrets are also deleted. If `false`, the `ownerReference` is disabled. If the `Kafka` resource is deleted when `false`, the CA Secrets are retained and available for reuse. Default is `true`."
            )
            .optional(),
          validityDays: z
            .number()
            .int()
            .gte(1)
            .describe(
              "The number of days generated certificates should be valid for. The default is 365."
            )
            .optional(),
          renewalDays: z
            .number()
            .int()
            .gte(1)
            .describe(
              "The number of days in the certificate renewal period. This is the number of days before the a certificate expires during which renewal actions may be performed. When `generateCertificateAuthority` is true, this will cause the generation of a new certificate. When `generateCertificateAuthority` is true, this will cause extra logging at WARN level about the pending certificate expiry. Default is 30."
            )
            .optional(),
          certificateExpirationPolicy: z
            .enum(["renew-certificate", "replace-key"])
            .describe(
              "How should CA certificate expiration be handled when `generateCertificateAuthority=true`. The default is for a new CA certificate to be generated reusing the existing private key."
            )
            .optional(),
        })
        .describe("Configuration of the cluster certificate authority.")
        .optional(),
      clientsCa: z
        .object({
          generateCertificateAuthority: z
            .boolean()
            .describe(
              "If true then Certificate Authority certificates will be generated automatically. Otherwise the user will need to provide a Secret with the CA certificate. Default is true."
            )
            .optional(),
          generateSecretOwnerReference: z
            .boolean()
            .describe(
              "If `true`, the Cluster and Client CA Secrets are configured with the `ownerReference` set to the `Kafka` resource. If the `Kafka` resource is deleted when `true`, the CA Secrets are also deleted. If `false`, the `ownerReference` is disabled. If the `Kafka` resource is deleted when `false`, the CA Secrets are retained and available for reuse. Default is `true`."
            )
            .optional(),
          validityDays: z
            .number()
            .int()
            .gte(1)
            .describe(
              "The number of days generated certificates should be valid for. The default is 365."
            )
            .optional(),
          renewalDays: z
            .number()
            .int()
            .gte(1)
            .describe(
              "The number of days in the certificate renewal period. This is the number of days before the a certificate expires during which renewal actions may be performed. When `generateCertificateAuthority` is true, this will cause the generation of a new certificate. When `generateCertificateAuthority` is true, this will cause extra logging at WARN level about the pending certificate expiry. Default is 30."
            )
            .optional(),
          certificateExpirationPolicy: z
            .enum(["renew-certificate", "replace-key"])
            .describe(
              "How should CA certificate expiration be handled when `generateCertificateAuthority=true`. The default is for a new CA certificate to be generated reusing the existing private key."
            )
            .optional(),
        })
        .describe("Configuration of the clients certificate authority.")
        .optional(),
      cruiseControl: z
        .object({
          image: z
            .string()
            .describe("The docker image for the pods.")
            .optional(),
          tlsSidecar: z
            .object({
              image: z
                .string()
                .describe("The docker image for the container.")
                .optional(),
              livenessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod liveness checking.")
                .optional(),
              logLevel: z
                .enum([
                  "emerg",
                  "alert",
                  "crit",
                  "err",
                  "warning",
                  "notice",
                  "info",
                  "debug",
                ])
                .describe(
                  "The log level for the TLS sidecar. Default value is `notice`."
                )
                .optional(),
              readinessProbe: z
                .object({
                  failureThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                    )
                    .optional(),
                  initialDelaySeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                    )
                    .optional(),
                  periodSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                    )
                    .optional(),
                  successThreshold: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                    )
                    .optional(),
                  timeoutSeconds: z
                    .number()
                    .int()
                    .gte(1)
                    .describe(
                      "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                    )
                    .optional(),
                })
                .describe("Pod readiness checking.")
                .optional(),
              resources: z
                .object({
                  claims: z
                    .array(z.object({ name: z.string().optional() }))
                    .optional(),
                  limits: z.record(z.any()).optional(),
                  requests: z.record(z.any()).optional(),
                })
                .describe("CPU and memory resources to reserve.")
                .optional(),
            })
            .describe("TLS sidecar configuration.")
            .optional(),
          resources: z
            .object({
              claims: z
                .array(z.object({ name: z.string().optional() }))
                .optional(),
              limits: z.record(z.any()).optional(),
              requests: z.record(z.any()).optional(),
            })
            .describe(
              "CPU and memory resources to reserve for the Cruise Control container."
            )
            .optional(),
          livenessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod liveness checking for the Cruise Control container.")
            .optional(),
          readinessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe(
              "Pod readiness checking for the Cruise Control container."
            )
            .optional(),
          jvmOptions: z
            .object({
              "-XX": z
                .record(z.any())
                .describe("A map of -XX options to the JVM.")
                .optional(),
              "-Xms": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xms option to to the JVM.")
                .optional(),
              "-Xmx": z
                .string()
                .regex(new RegExp("^[0-9]+[mMgG]?$"))
                .describe("-Xmx option to to the JVM.")
                .optional(),
              gcLoggingEnabled: z
                .boolean()
                .describe(
                  "Specifies whether the Garbage Collection logging is enabled. The default is false."
                )
                .optional(),
              javaSystemProperties: z
                .array(
                  z.object({
                    name: z
                      .string()
                      .describe("The system property name.")
                      .optional(),
                    value: z
                      .string()
                      .describe("The system property value.")
                      .optional(),
                  })
                )
                .describe(
                  "A map of additional system properties which will be passed using the `-D` option to the JVM."
                )
                .optional(),
            })
            .describe("JVM Options for the Cruise Control container.")
            .optional(),
          logging: z
            .object({
              loggers: z
                .record(z.any())
                .describe("A Map from logger name to logger level.")
                .optional(),
              type: z
                .enum(["inline", "external"])
                .describe(
                  "Logging type, must be either 'inline' or 'external'."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "`ConfigMap` entry where the logging configuration is stored. "
                )
                .optional(),
            })
            .describe("Logging configuration (Log4j 2) for Cruise Control.")
            .optional(),
          template: z
            .object({
              deployment: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  deploymentStrategy: z
                    .enum(["RollingUpdate", "Recreate"])
                    .describe(
                      "Pod replacement strategy for deployment configuration changes. Valid values are `RollingUpdate` and `Recreate`. Defaults to `RollingUpdate`."
                    )
                    .optional(),
                })
                .describe("Template for Cruise Control `Deployment`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for Cruise Control `Pods`.")
                .optional(),
              apiService: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  ipFamilyPolicy: z
                    .enum([
                      "SingleStack",
                      "PreferDualStack",
                      "RequireDualStack",
                    ])
                    .describe(
                      "Specifies the IP Family Policy used by the service. Available options are `SingleStack`, `PreferDualStack` and `RequireDualStack`. `SingleStack` is for a single IP family. `PreferDualStack` is for two IP families on dual-stack configured clusters or a single IP family on single-stack clusters. `RequireDualStack` fails unless there are two IP families on dual-stack configured clusters. If unspecified, Kubernetes will choose the default value based on the service type. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                  ipFamilies: z
                    .array(z.enum(["IPv4", "IPv6"]))
                    .describe(
                      "Specifies the IP Families used by the service. Available options are `IPv4` and `IPv6. If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. Available on Kubernetes 1.20 and newer."
                    )
                    .optional(),
                })
                .describe("Template for Cruise Control API `Service`.")
                .optional(),
              podDisruptionBudget: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe(
                      "Metadata to apply to the `PodDisruptionBudgetTemplate` resource."
                    )
                    .optional(),
                  maxUnavailable: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "Maximum number of unavailable pods to allow automatic Pod eviction. A Pod eviction is allowed when the `maxUnavailable` number of pods or fewer are unavailable after the eviction. Setting this value to 0 prevents all voluntary evictions, so the pods must be evicted manually. Defaults to 1."
                    )
                    .optional(),
                })
                .describe("Template for Cruise Control `PodDisruptionBudget`.")
                .optional(),
              cruiseControlContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Cruise Control container.")
                .optional(),
              tlsSidecarContainer: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe(
                  "Template for the Cruise Control TLS sidecar container."
                )
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Cruise Control service account.")
                .optional(),
            })
            .describe(
              "Template to specify how Cruise Control resources, `Deployments` and `Pods`, are generated."
            )
            .optional(),
          brokerCapacity: z
            .object({
              disk: z
                .string()
                .regex(new RegExp("^[0-9]+([.][0-9]*)?([KMGTPE]i?|e[0-9]+)?$"))
                .describe(
                  "Broker capacity for disk in bytes. Use a number value with either standard Kubernetes byte units (K, M, G, or T), their bibyte (power of two) equivalents (Ki, Mi, Gi, or Ti), or a byte value with or without E notation. For example, 100000M, 100000Mi, 104857600000, or 1e+11."
                )
                .optional(),
              cpuUtilization: z
                .number()
                .int()
                .gte(0)
                .lte(100)
                .describe(
                  "Broker capacity for CPU resource utilization as a percentage (0 - 100)."
                )
                .optional(),
              cpu: z
                .string()
                .regex(new RegExp("^[0-9]+([.][0-9]{0,3}|[m]?)$"))
                .describe(
                  "Broker capacity for CPU resource in cores or millicores. For example, 1, 1.500, 1500m. For more information on valid CPU resource units see https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#meaning-of-cpu."
                )
                .optional(),
              inboundNetwork: z
                .string()
                .regex(new RegExp("^[0-9]+([KMG]i?)?B/s$"))
                .describe(
                  "Broker capacity for inbound network throughput in bytes per second. Use an integer value with standard Kubernetes byte units (K, M, G) or their bibyte (power of two) equivalents (Ki, Mi, Gi) per second. For example, 10000KiB/s."
                )
                .optional(),
              outboundNetwork: z
                .string()
                .regex(new RegExp("^[0-9]+([KMG]i?)?B/s$"))
                .describe(
                  "Broker capacity for outbound network throughput in bytes per second. Use an integer value with standard Kubernetes byte units (K, M, G) or their bibyte (power of two) equivalents (Ki, Mi, Gi) per second. For example, 10000KiB/s."
                )
                .optional(),
              overrides: z
                .array(
                  z.object({
                    brokers: z
                      .array(z.number().int())
                      .describe("List of Kafka brokers (broker identifiers)."),
                    cpu: z
                      .string()
                      .regex(new RegExp("^[0-9]+([.][0-9]{0,3}|[m]?)$"))
                      .describe(
                        "Broker capacity for CPU resource in cores or millicores. For example, 1, 1.500, 1500m. For more information on valid CPU resource units see https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#meaning-of-cpu."
                      )
                      .optional(),
                    inboundNetwork: z
                      .string()
                      .regex(new RegExp("^[0-9]+([KMG]i?)?B/s$"))
                      .describe(
                        "Broker capacity for inbound network throughput in bytes per second. Use an integer value with standard Kubernetes byte units (K, M, G) or their bibyte (power of two) equivalents (Ki, Mi, Gi) per second. For example, 10000KiB/s."
                      )
                      .optional(),
                    outboundNetwork: z
                      .string()
                      .regex(new RegExp("^[0-9]+([KMG]i?)?B/s$"))
                      .describe(
                        "Broker capacity for outbound network throughput in bytes per second. Use an integer value with standard Kubernetes byte units (K, M, G) or their bibyte (power of two) equivalents (Ki, Mi, Gi) per second. For example, 10000KiB/s."
                      )
                      .optional(),
                  })
                )
                .describe(
                  "Overrides for individual brokers. The `overrides` property lets you specify a different capacity configuration for different brokers."
                )
                .optional(),
            })
            .describe("The Cruise Control `brokerCapacity` configuration.")
            .optional(),
          config: z
            .record(z.any())
            .describe(
              "The Cruise Control configuration. For a full list of configuration options refer to https://github.com/linkedin/cruise-control/wiki/Configurations. Note that properties with the following prefixes cannot be set: bootstrap.servers, client.id, zookeeper., network., security., failed.brokers.zk.path,webserver.http., webserver.api.urlprefix, webserver.session.path, webserver.accesslog., two.step., request.reason.required,metric.reporter.sampler.bootstrap.servers, capacity.config.file, self.healing., ssl., kafka.broker.failure.detection.enable, topic.config.provider.class (with the exception of: ssl.cipher.suites, ssl.protocol, ssl.enabled.protocols, webserver.http.cors.enabled, webserver.http.cors.origin, webserver.http.cors.exposeheaders, webserver.security.enable, webserver.ssl.enable)."
            )
            .optional(),
          metricsConfig: z
            .object({
              type: z
                .enum(["jmxPrometheusExporter"])
                .describe(
                  "Metrics type. Only 'jmxPrometheusExporter' supported currently."
                ),
              valueFrom: z
                .object({
                  configMapKeyRef: z
                    .object({
                      key: z.string().optional(),
                      name: z.string().optional(),
                      optional: z.boolean().optional(),
                    })
                    .describe(
                      "Reference to the key in the ConfigMap containing the configuration."
                    )
                    .optional(),
                })
                .describe(
                  "ConfigMap entry where the Prometheus JMX Exporter configuration is stored. For details of the structure of this configuration, see the {JMXExporter}."
                ),
            })
            .describe("Metrics configuration.")
            .optional(),
        })
        .describe(
          "Configuration for Cruise Control deployment. Deploys a Cruise Control instance when specified."
        )
        .optional(),
      jmxTrans: z
        .object({
          image: z
            .string()
            .describe("The image to use for the JmxTrans.")
            .optional(),
          outputDefinitions: z
            .array(
              z.object({
                outputType: z
                  .string()
                  .describe(
                    "Template for setting the format of the data that will be pushed.For more information see https://github.com/jmxtrans/jmxtrans/wiki/OutputWriters[JmxTrans OutputWriters]."
                  ),
                host: z
                  .string()
                  .describe(
                    "The DNS/hostname of the remote host that the data is pushed to."
                  )
                  .optional(),
                port: z
                  .number()
                  .int()
                  .describe(
                    "The port of the remote host that the data is pushed to."
                  )
                  .optional(),
                flushDelayInSeconds: z
                  .number()
                  .int()
                  .describe(
                    "How many seconds the JmxTrans waits before pushing a new set of data out."
                  )
                  .optional(),
                typeNames: z
                  .array(z.string())
                  .describe(
                    "Template for filtering data to be included in response to a wildcard query. For more information see https://github.com/jmxtrans/jmxtrans/wiki/Queries[JmxTrans queries]."
                  )
                  .optional(),
                name: z
                  .string()
                  .describe(
                    "Template for setting the name of the output definition. This is used to identify where to send the results of queries should be sent."
                  ),
              })
            )
            .describe(
              "Defines the output hosts that will be referenced later on. For more information on these properties see, xref:type-JmxTransOutputDefinitionTemplate-reference[`JmxTransOutputDefinitionTemplate` schema reference]."
            ),
          logLevel: z
            .string()
            .describe(
              "Sets the logging level of the JmxTrans deployment.For more information see, https://github.com/jmxtrans/jmxtrans-agent/wiki/Troubleshooting[JmxTrans Logging Level]."
            )
            .optional(),
          kafkaQueries: z
            .array(
              z.object({
                targetMBean: z
                  .string()
                  .describe(
                    "If using wildcards instead of a specific MBean then the data is gathered from multiple MBeans. Otherwise if specifying an MBean then data is gathered from that specified MBean."
                  ),
                attributes: z
                  .array(z.string())
                  .describe(
                    "Determine which attributes of the targeted MBean should be included."
                  ),
                outputs: z
                  .array(z.string())
                  .describe(
                    "List of the names of output definitions specified in the spec.kafka.jmxTrans.outputDefinitions that have defined where JMX metrics are pushed to, and in which data format."
                  ),
              })
            )
            .describe(
              "Queries to send to the Kafka brokers to define what data should be read from each broker. For more information on these properties see, xref:type-JmxTransQueryTemplate-reference[`JmxTransQueryTemplate` schema reference]."
            ),
          resources: z
            .object({
              claims: z
                .array(z.object({ name: z.string().optional() }))
                .optional(),
              limits: z.record(z.any()).optional(),
              requests: z.record(z.any()).optional(),
            })
            .describe("CPU and memory resources to reserve.")
            .optional(),
          template: z
            .object({
              deployment: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  deploymentStrategy: z
                    .enum(["RollingUpdate", "Recreate"])
                    .describe(
                      "Pod replacement strategy for deployment configuration changes. Valid values are `RollingUpdate` and `Recreate`. Defaults to `RollingUpdate`."
                    )
                    .optional(),
                })
                .describe("Template for JmxTrans `Deployment`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for JmxTrans `Pods`.")
                .optional(),
              container: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for JmxTrans container.")
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the JmxTrans service account.")
                .optional(),
            })
            .describe("Template for JmxTrans resources.")
            .optional(),
        })
        .describe(
          "As of Strimzi 0.35.0, JMXTrans is not supported anymore and this option is ignored."
        )
        .optional(),
      kafkaExporter: z
        .object({
          image: z
            .string()
            .describe("The docker image for the pods.")
            .optional(),
          groupRegex: z
            .string()
            .describe(
              "Regular expression to specify which consumer groups to collect. Default value is `.*`."
            )
            .optional(),
          topicRegex: z
            .string()
            .describe(
              "Regular expression to specify which topics to collect. Default value is `.*`."
            )
            .optional(),
          resources: z
            .object({
              claims: z
                .array(z.object({ name: z.string().optional() }))
                .optional(),
              limits: z.record(z.any()).optional(),
              requests: z.record(z.any()).optional(),
            })
            .describe("CPU and memory resources to reserve.")
            .optional(),
          logging: z
            .string()
            .describe(
              "Only log messages with the given severity or above. Valid levels: [`info`, `debug`, `trace`]. Default log level is `info`."
            )
            .optional(),
          enableSaramaLogging: z
            .boolean()
            .describe(
              "Enable Sarama logging, a Go client library used by the Kafka Exporter."
            )
            .optional(),
          template: z
            .object({
              deployment: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  deploymentStrategy: z
                    .enum(["RollingUpdate", "Recreate"])
                    .describe(
                      "Pod replacement strategy for deployment configuration changes. Valid values are `RollingUpdate` and `Recreate`. Defaults to `RollingUpdate`."
                    )
                    .optional(),
                })
                .describe("Template for Kafka Exporter `Deployment`.")
                .optional(),
              pod: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                  imagePullSecrets: z
                    .array(z.object({ name: z.string().optional() }))
                    .describe(
                      "List of references to secrets in the same namespace to use for pulling any of the images used by this Pod. When the `STRIMZI_IMAGE_PULL_SECRETS` environment variable in Cluster Operator and the `imagePullSecrets` option are specified, only the `imagePullSecrets` variable is used and the `STRIMZI_IMAGE_PULL_SECRETS` variable is ignored."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      fsGroup: z.number().int().optional(),
                      fsGroupChangePolicy: z.string().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      supplementalGroups: z.array(z.number().int()).optional(),
                      sysctls: z
                        .array(
                          z.object({
                            name: z.string().optional(),
                            value: z.string().optional(),
                          })
                        )
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe(
                      "Configures pod-level security attributes and common container settings."
                    )
                    .optional(),
                  terminationGracePeriodSeconds: z
                    .number()
                    .int()
                    .gte(0)
                    .describe(
                      "The grace period is the duration in seconds after the processes running in the pod are sent a termination signal, and the time when the processes are forcibly halted with a kill signal. Set this value to longer than the expected cleanup time for your process. Value must be a non-negative integer. A zero value indicates delete immediately. You might need to increase the grace period for very large Kafka clusters, so that the Kafka brokers have enough time to transfer their work to another broker before they are terminated. Defaults to 30 seconds."
                    )
                    .optional(),
                  affinity: z
                    .object({
                      nodeAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                preference: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .object({
                              nodeSelectorTerms: z
                                .array(
                                  z.object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchFields: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                  })
                                )
                                .optional(),
                            })
                            .optional(),
                        })
                        .optional(),
                      podAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                      podAntiAffinity: z
                        .object({
                          preferredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                podAffinityTerm: z
                                  .object({
                                    labelSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaceSelector: z
                                      .object({
                                        matchExpressions: z
                                          .array(
                                            z.object({
                                              key: z.string().optional(),
                                              operator: z.string().optional(),
                                              values: z
                                                .array(z.string())
                                                .optional(),
                                            })
                                          )
                                          .optional(),
                                        matchLabels: z
                                          .record(z.any())
                                          .optional(),
                                      })
                                      .optional(),
                                    namespaces: z.array(z.string()).optional(),
                                    topologyKey: z.string().optional(),
                                  })
                                  .optional(),
                                weight: z.number().int().optional(),
                              })
                            )
                            .optional(),
                          requiredDuringSchedulingIgnoredDuringExecution: z
                            .array(
                              z.object({
                                labelSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaceSelector: z
                                  .object({
                                    matchExpressions: z
                                      .array(
                                        z.object({
                                          key: z.string().optional(),
                                          operator: z.string().optional(),
                                          values: z
                                            .array(z.string())
                                            .optional(),
                                        })
                                      )
                                      .optional(),
                                    matchLabels: z.record(z.any()).optional(),
                                  })
                                  .optional(),
                                namespaces: z.array(z.string()).optional(),
                                topologyKey: z.string().optional(),
                              })
                            )
                            .optional(),
                        })
                        .optional(),
                    })
                    .describe("The pod's affinity rules.")
                    .optional(),
                  tolerations: z
                    .array(
                      z.object({
                        effect: z.string().optional(),
                        key: z.string().optional(),
                        operator: z.string().optional(),
                        tolerationSeconds: z.number().int().optional(),
                        value: z.string().optional(),
                      })
                    )
                    .describe("The pod's tolerations.")
                    .optional(),
                  priorityClassName: z
                    .string()
                    .describe(
                      "The name of the priority class used to assign priority to the pods. For more information about priority classes, see {K8sPriorityClass}."
                    )
                    .optional(),
                  schedulerName: z
                    .string()
                    .describe(
                      "The name of the scheduler used to dispatch this `Pod`. If not specified, the default scheduler will be used."
                    )
                    .optional(),
                  hostAliases: z
                    .array(
                      z.object({
                        hostnames: z.array(z.string()).optional(),
                        ip: z.string().optional(),
                      })
                    )
                    .describe(
                      "The pod's HostAliases. HostAliases is an optional list of hosts and IPs that will be injected into the Pod's hosts file if specified."
                    )
                    .optional(),
                  tmpDirSizeLimit: z
                    .string()
                    .regex(
                      new RegExp("^([0-9.]+)([eEinumkKMGTP]*[-+]?[0-9]*)$")
                    )
                    .describe(
                      "Defines the total amount (for example `1Gi`) of local storage required for temporary EmptyDir volume (`/tmp`). Default value is `5Mi`."
                    )
                    .optional(),
                  enableServiceLinks: z
                    .boolean()
                    .describe(
                      "Indicates whether information about services should be injected into Pod's environment variables."
                    )
                    .optional(),
                  topologySpreadConstraints: z
                    .array(
                      z.object({
                        labelSelector: z
                          .object({
                            matchExpressions: z
                              .array(
                                z.object({
                                  key: z.string().optional(),
                                  operator: z.string().optional(),
                                  values: z.array(z.string()).optional(),
                                })
                              )
                              .optional(),
                            matchLabels: z.record(z.any()).optional(),
                          })
                          .optional(),
                        matchLabelKeys: z.array(z.string()).optional(),
                        maxSkew: z.number().int().optional(),
                        minDomains: z.number().int().optional(),
                        nodeAffinityPolicy: z.string().optional(),
                        nodeTaintsPolicy: z.string().optional(),
                        topologyKey: z.string().optional(),
                        whenUnsatisfiable: z.string().optional(),
                      })
                    )
                    .describe("The pod's topology spread constraints.")
                    .optional(),
                })
                .describe("Template for Kafka Exporter `Pods`.")
                .optional(),
              service: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for Kafka Exporter `Service`.")
                .optional(),
              container: z
                .object({
                  env: z
                    .array(
                      z.object({
                        name: z
                          .string()
                          .describe("The environment variable key.")
                          .optional(),
                        value: z
                          .string()
                          .describe("The environment variable value.")
                          .optional(),
                      })
                    )
                    .describe(
                      "Environment variables which should be applied to the container."
                    )
                    .optional(),
                  securityContext: z
                    .object({
                      allowPrivilegeEscalation: z.boolean().optional(),
                      capabilities: z
                        .object({
                          add: z.array(z.string()).optional(),
                          drop: z.array(z.string()).optional(),
                        })
                        .optional(),
                      privileged: z.boolean().optional(),
                      procMount: z.string().optional(),
                      readOnlyRootFilesystem: z.boolean().optional(),
                      runAsGroup: z.number().int().optional(),
                      runAsNonRoot: z.boolean().optional(),
                      runAsUser: z.number().int().optional(),
                      seLinuxOptions: z
                        .object({
                          level: z.string().optional(),
                          role: z.string().optional(),
                          type: z.string().optional(),
                          user: z.string().optional(),
                        })
                        .optional(),
                      seccompProfile: z
                        .object({
                          localhostProfile: z.string().optional(),
                          type: z.string().optional(),
                        })
                        .optional(),
                      windowsOptions: z
                        .object({
                          gmsaCredentialSpec: z.string().optional(),
                          gmsaCredentialSpecName: z.string().optional(),
                          hostProcess: z.boolean().optional(),
                          runAsUserName: z.string().optional(),
                        })
                        .optional(),
                    })
                    .describe("Security context for the container.")
                    .optional(),
                })
                .describe("Template for the Kafka Exporter container.")
                .optional(),
              serviceAccount: z
                .object({
                  metadata: z
                    .object({
                      labels: z
                        .record(z.any())
                        .describe("Labels added to the Kubernetes resource.")
                        .optional(),
                      annotations: z
                        .record(z.any())
                        .describe(
                          "Annotations added to the Kubernetes resource."
                        )
                        .optional(),
                    })
                    .describe("Metadata applied to the resource.")
                    .optional(),
                })
                .describe("Template for the Kafka Exporter service account.")
                .optional(),
            })
            .describe("Customization of deployment templates and pods.")
            .optional(),
          livenessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod liveness check.")
            .optional(),
          readinessProbe: z
            .object({
              failureThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive failures for the probe to be considered failed after having succeeded. Defaults to 3. Minimum value is 1."
                )
                .optional(),
              initialDelaySeconds: z
                .number()
                .int()
                .gte(0)
                .describe(
                  "The initial delay before first the health is first checked. Default to 15 seconds. Minimum value is 0."
                )
                .optional(),
              periodSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1."
                )
                .optional(),
              successThreshold: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "Minimum consecutive successes for the probe to be considered successful after having failed. Defaults to 1. Must be 1 for liveness. Minimum value is 1."
                )
                .optional(),
              timeoutSeconds: z
                .number()
                .int()
                .gte(1)
                .describe(
                  "The timeout for each attempted health check. Default to 5 seconds. Minimum value is 1."
                )
                .optional(),
            })
            .describe("Pod readiness check.")
            .optional(),
        })
        .describe(
          "Configuration of the Kafka Exporter. Kafka Exporter can provide additional metrics, for example lag of consumer group at topic/partition."
        )
        .optional(),
      maintenanceTimeWindows: z
        .array(z.string())
        .describe(
          "A list of time windows for maintenance tasks (that is, certificates renewal). Each time window is defined by a cron expression."
        )
        .optional(),
    })
    .describe(
      "The specification of the Kafka and ZooKeeper clusters, and Topic Operator."
    )
    .optional(),
  status: z
    .object({
      conditions: z
        .array(
          z.object({
            type: z
              .string()
              .describe(
                "The unique identifier of a condition, used to distinguish between other conditions in the resource."
              )
              .optional(),
            status: z
              .string()
              .describe(
                "The status of the condition, either True, False or Unknown."
              )
              .optional(),
            lastTransitionTime: z
              .string()
              .describe(
                "Last time the condition of a type changed from one status to another. The required format is 'yyyy-MM-ddTHH:mm:ssZ', in the UTC time zone."
              )
              .optional(),
            reason: z
              .string()
              .describe(
                "The reason for the condition's last transition (a single word in CamelCase)."
              )
              .optional(),
            message: z
              .string()
              .describe(
                "Human-readable message indicating details about the condition's last transition."
              )
              .optional(),
          })
        )
        .describe("List of status conditions.")
        .optional(),
      observedGeneration: z
        .number()
        .int()
        .describe(
          "The generation of the CRD that was last reconciled by the operator."
        )
        .optional(),
      listeners: z
        .array(
          z.object({
            type: z
              .string()
              .describe(
                "*The `type` property has been deprecated, and should now be configured using `name`.* The name of the listener."
              )
              .optional(),
            name: z.string().describe("The name of the listener.").optional(),
            addresses: z
              .array(
                z.object({
                  host: z
                    .string()
                    .describe(
                      "The DNS name or IP address of the Kafka bootstrap service."
                    )
                    .optional(),
                  port: z
                    .number()
                    .int()
                    .describe("The port of the Kafka bootstrap service.")
                    .optional(),
                })
              )
              .describe("A list of the addresses for this listener.")
              .optional(),
            bootstrapServers: z
              .string()
              .describe(
                "A comma-separated list of `host:port` pairs for connecting to the Kafka cluster using this listener."
              )
              .optional(),
            certificates: z
              .array(z.string())
              .describe(
                "A list of TLS certificates which can be used to verify the identity of the server when connecting to the given listener. Set only for `tls` and `external` listeners."
              )
              .optional(),
          })
        )
        .describe("Addresses of the internal and external listeners.")
        .optional(),
      clusterId: z.string().describe("Kafka cluster Id.").optional(),
    })
    .describe(
      "The status of the Kafka and ZooKeeper clusters, and Topic Operator."
    )
    .optional(),
});
