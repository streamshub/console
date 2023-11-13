package com.github.eyefloaters.console.api.model;

import java.io.IOException;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;
import com.github.eyefloaters.console.api.support.KafkaUuid;

@JsonInclude(value = Include.NON_NULL)
public record OffsetAndMetadata(
        @NotBlank(payload = ErrorCategory.InvalidResource.class)
        @KafkaUuid(payload = ErrorCategory.InvalidResource.class, message = "Invalid topic identifier")
        String topicId,

        @Schema(readOnly = true)
        String topicName,

        @Min(value = 0, payload = ErrorCategory.InvalidResource.class)
        @Schema(
            nullable = true,
            description = """
                The partition within the topic to which this offset and metadata are applicable. When
                resetting offsets, a null partition indicates that the offset and metadata shall be committed
                for all partitions in the topic.
                """)
        Integer partition,

        @Schema(
            description = """
                When describing a consumer group, the last committed offset for the topic and partitions in
                the consumer group being described.

                When resetting offsets for a partition, the value may be a literal offset,
                or any valid offset specification. The offset for the partition in the consumer
                group will be reset accordingly.

                1. `earliest` - reset to the earliest/first offset
                2. `latest` - reset to the latest/last offset available
                3. `maxTimestamp` - reset to the offset having the greatest timestamp
                4. literal timestamp - reset to the offset having a timestamp equal to or greater than the given timestamp

                If no offset exists matching the `maxTimestamp` or a literal timestamp spec, no changes will
                be made to the consumer group offset(s) for the partition.
                """,
            implementation = Object.class,
            oneOf = { Long.class, OffsetSpec.class })
        @JsonDeserialize(using = OffsetAndMetadata.EitherLongOrStringDeserializer.class)
        @NotNull(payload = ErrorCategory.InvalidResource.class)
        Either<
            @Min(value = 0, payload = ErrorCategory.InvalidResource.class)
            Long,
            @KafkaOffsetSpec(payload = ErrorCategory.InvalidResource.class)
            String
        > offset,

        @Schema(readOnly = true)
        long lag,

        String metadata,

        Integer leaderEpoch
) {

    @Schema(ref = "OffsetSpec")
    private static class OffsetSpec { }

    static class EitherLongOrStringDeserializer extends StdDeserializer<Either<Long, String>> {
        private static final long serialVersionUID = 1L;

        public EitherLongOrStringDeserializer() {
            this(null);
        }

        protected EitherLongOrStringDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Either<Long, String> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);

            if (node.isNumber()) {
                return Either.of(node.asLong());
            } else if (node.isTextual()) {
                return Either.ofAlternate(node.asText());
            }

            throw MismatchedInputException.from(parser, Either.class, "Unable to parse offset");
        }
    }
}
