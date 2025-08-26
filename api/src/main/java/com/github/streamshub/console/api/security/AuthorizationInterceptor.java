package com.github.streamshub.console.api.security;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;

import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.ClientFactory;
import com.github.streamshub.console.api.service.TopicDescribeService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.support.Identifiers;

import io.quarkus.security.identity.SecurityIdentity;

@Authorized
@Priority(1)
@Interceptor
@Dependent
public class AuthorizationInterceptor {

    @Inject
    Logger logger;

    @Inject
    Map<String, KafkaContext> contexts;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    UriInfo requestUri;

    @Inject
    TopicDescribeService topicDescribe;

    @AroundInvoke
    Object authorize(InvocationContext context) throws Exception {
        ResourcePrivilege authz = context.getMethod().getAnnotation(ResourcePrivilege.class);
        var requiredPermission = getRequiredPermission(authz);

        boolean allow = securityIdentity.checkPermission(requiredPermission)
                .subscribeAsCompletionStage()
                .join();

        if (!allow) {
            throw new ForbiddenException("Access denied");
        }

        return context.proceed();
    }

    /**
     * Pull the resource type and resource name from the request URI path to be used
     * to determine authorization. The path is transformed as follows.
     *
     * <p>
     * Given a resource path `/api/kafkas/xyz/topics/abc/records`:
     *
     * <ol>
     * <li>Skip the leading `/api` segment
     * <li>Append segments `kafkas/xyz/topics` to the resource type
     * <li>Use segment `abc` as the resource name
     * <li>Append segment `/records` to the resource type
     * </ol>
     *
     * <p>
     * For a principal to be authorized to access the resource, they must be a member
     * of a role with access to `kafkas` `xyz` (named or all `kafkas`), and further
     * with access to resource `topics/records` `abc` (named or all `topics/records`).
     *
     * @param resource target resource type builder
     * @param resourceNames collection to hold the resource name
     */
    private ConsolePermission getRequiredPermission(ResourcePrivilege authz) {
        var segments = requestUri.getPathSegments();
        var segmentCount = segments.size();

        // skip the first segment `/api`
        String rootResource = segments.get(1).getPath();

        String resource = rootResource;
        String resourceDisplay = null;
        List<String> resourceNames = new ArrayList<>(1);
        List<String> resourceNamesDisplay = null;

        if (ResourceTypes.Global.KAFKAS.value().equals(rootResource)) {
            if (segmentCount > 2) {
                String kafkaId = segments.get(2).getPath();
                KafkaContext ctx = Optional.ofNullable(contexts.get(kafkaId))
                        .orElseThrow(() -> ClientFactory.NO_SUCH_KAFKA.apply(kafkaId));

                /*
                 * For URLs like `/api/kafkas/123`, the Kafka ID is the resource name
                 * and is configured at the top-level `security` key in the console's
                 * configuration. Otherwise, the Kafka ID is appended to the resource
                 * path and the configuration originates from the Kafka-level `security`
                 * key, scoped to the Kafka cluster under which it is specified.
                 */
                if (segmentCount > 3) {
                    StringBuilder resourceBuilder = new StringBuilder();
                    setKafkaResource(resourceBuilder, resourceNames, segments);
                    String rawResource = resourceBuilder.toString();
                    resource = ctx.securityResourcePath(rawResource);
                    resourceDisplay = ctx.auditDisplayResourcePath(rawResource);
                } else {
                    resourceNames.add(kafkaId);
                    resourceNamesDisplay = List.of(ctx.clusterConfig().clusterKey());
                }
            }
        } else {
            if (segmentCount > 2) {
                resourceNames.add(segments.get(2).getPath());
            }
        }

        return new ConsolePermission(resource, resourceDisplay, resourceNames, authz.value())
                .resourceNamesDisplay(resourceNamesDisplay);
    }

    private void setKafkaResource(StringBuilder resource, List<String> resourceNames, List<PathSegment> segments) {
        var segmentCount = segments.size();
        UnaryOperator<String> converter = UnaryOperator.identity();

        for (int s = 3; s < segmentCount; s++) {
            String segment = segments.get(s).getPath();

            if (s == 4) {
                resourceNames.add(converter.apply(segment));
            } else {
                if (s == 3) {
                    switch (ResourceTypes.Kafka.fromValue(segment)) {
                        case CONSUMER_GROUPS:
                            converter = this::consumerGroupId;
                            break;
                        case REBALANCES:
                            converter = this::rebalanceName;
                            break;
                        case TOPICS:
                            converter = this::topicName;
                            break;
                        default:
                            break;
                    }
                }
                if (!resource.isEmpty()) {
                    resource.append('/');
                }
                resource.append(segment);
            }
        }
    }

    /**
     * Attempt to cross-reference the topic ID to the topic name which is used to
     * configure topic-level authorization.
     */
    private String topicName(String topicId) {
        return topicDescribe.topicNameForId(topicId).toCompletableFuture().join()
            .orElseThrow(() -> new UnknownTopicIdException("No such topic: " + topicId));
    }

    /**
     * Decode the base64-encoded consumer groupId.
     */
    private String consumerGroupId(String groupId) {
        String[] decoded = Identifiers.decode(groupId);
        if (decoded.length != 1) {
            throw new BadRequestException("Malformed consumer group URI");
        }
        return decoded[0];
    }

    /**
     * Extract the Kafka Rebalance name from the encoded rebalanceId.
     */
    private String rebalanceName(String rebalanceId) {
        String decodedId = new String(Base64.getUrlDecoder().decode(rebalanceId));
        String[] idElements = decodedId.split("/");

        if (idElements.length != 2) {
            throw new NotFoundException("No such rebalance: " + rebalanceId);
        }

        return idElements[1];
    }
}
