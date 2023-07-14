package org.bf2.admin.kafka.admin.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.common.ConsumerGroupState;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.bf2.admin.kafka.admin.model.Types;
import org.bf2.admin.kafka.admin.model.Types.ConsumerGroupMetrics;
import org.bf2.admin.kafka.admin.model.Types.ConsumerGroupOffsetResetParameters.OffsetType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class OASModelFilter implements OASFilter {

    private static final String SECURITY_SCHEME_NAME_OAUTH = "OAuth2";
    private static final String SECURITY_SCHEME_NAME_BASIC = "BasicAuth";

    @Override
    public Schema filterSchema(Schema schema) {
        List<Schema> allOf = schema.getAllOf();

        // Remove superfluous `nullable: false` added by OpenAPI scanner
        if (allOf != null && allOf.stream().anyMatch(s -> Boolean.FALSE.equals(s.getNullable()))) {
            allOf.stream()
                .filter(s -> s.getRef() != null)
                .findFirst()
                .ifPresent(ref -> {
                    schema.setRef(ref.getRef());
                    schema.setAllOf(null);
                });
        }

        return OASFilter.super.filterSchema(schema);
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Config config = ConfigProvider.getConfig();

        if (config.getOptionalValue(KafkaAdminConfigRetriever.OAUTH_ENABLED, Boolean.class).orElse(true)) {
            Optional<String> tokenUrl = config.getOptionalValue(KafkaAdminConfigRetriever.OAUTH_TOKEN_ENDPOINT_URI, String.class);

            openAPI.getComponents()
                    .getSecuritySchemes()
                    .get(SECURITY_SCHEME_NAME_OAUTH)
                    .getFlows()
                    .getClientCredentials()
                    .setTokenUrl(tokenUrl.orElse(null));
        } else if (config.getOptionalValue(KafkaAdminConfigRetriever.BASIC_ENABLED, Boolean.class).orElse(false)) {
            openAPI.setSecurity(List.of(OASFactory.createSecurityRequirement()
                                        .addScheme(SECURITY_SCHEME_NAME_BASIC, Collections.emptyList())));
            openAPI.getComponents()
                .setSecuritySchemes(Map.of(SECURITY_SCHEME_NAME_BASIC,
                                           OASFactory.createSecurityScheme().type(Type.HTTP).scheme("basic")));
        } else {
            openAPI.setSecurity(null);
            openAPI.getComponents().setSecuritySchemes(null);
        }

        // Sort global schemas
        openAPI.getComponents().setSchemas(new TreeMap<>(openAPI.getComponents().getSchemas()));
        var info = openAPI.getInfo();

        info.setTitle("Kafka Instance API");
        info.setDescription("API for interacting with Kafka Instance. Includes Produce, Consume and Admin APIs");

        config.getOptionalValue("kafka.admin.num.partitions.max", String.class)
            .map(BigDecimal::new)
            .ifPresent(openAPI.getComponents().getSchemas().get("TopicSettings").getProperties().get("numPartitions")::setMaximum);

        generateExamples().forEach(openAPI.getComponents()::addExample);
    }

    Map<String, Example> generateExamples() {
        ObjectMapper mapper = new ObjectMapper();

        var newTopicExample = new Types.NewTopic("my-topic",
                   new Types.TopicSettings(3, List.of(
                           new Types.ConfigEntry("min.insync.replicas", "1"),
                           new Types.ConfigEntry("max.message.bytes", "1050000"))));

        var consumerGroupExample = new Types.ConsumerGroup("consumer_group_1",
                   ConsumerGroupState.STABLE,
                   List.of(new Types.Consumer("consumer_group_member1", "consumer_group_1", "topic-1", 0, 5, 0, 5),
                           new Types.Consumer("consumer_group_member2", "consumer_group_1", "topic-1", 1, 3, 0, 3),
                           new Types.Consumer("consumer_group_member3", "consumer_group_1", "topic-1", 2, 5, 1, 6)),
                   new ConsumerGroupMetrics(0, 3, 0));

        var consumerGroupResetExample = new Types.ConsumerGroupOffsetResetParameters(OffsetType.ABSOLUTE, "4",
                   List.of(new Types.TopicsToResetOffset("my-topic", List.of(0))));

        var recordProduceExample = new Types.Record("my-topic", 1, null, Map.of("X-Custom-Header", "header-value-1"), null, "{ \"examplekey\": \"example-value\" }");

        Map<String, Example> examples = new LinkedHashMap<>();

        examples.put("NewTopicExample", createExample(mapper, newTopicExample, "Sample new topic with 3 partitions"));
        examples.put("ConsumerGroupExample", createExample(mapper, consumerGroupExample, "Sample consumer group with 3 partitions and 3 active consumers"));
        examples.put("ConsumerGroupOffsetResetExample", createExample(mapper, consumerGroupResetExample, "Sample request to reset partition `0` of topic `my-topic` to offset `4`"));
        examples.put("RecordProduceExample", createExample(mapper, recordProduceExample, "Sample record to produce a record to partition 1, including a custom header"));

        return examples;
    }

    Example createExample(ObjectMapper mapper, Object value, String description) {
        return OASFactory.createExample()
                .description(description)
                .value(mapper.convertValue(value, ObjectNode.class));
    }
}
