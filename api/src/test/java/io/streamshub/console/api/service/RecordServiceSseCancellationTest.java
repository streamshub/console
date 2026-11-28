package io.streamshub.console.api.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import io.streamshub.console.api.model.RecordFilterParams;
import io.streamshub.console.api.support.KafkaContext;
import io.streamshub.console.api.support.serdes.RecordData;

import io.quarkus.security.identity.SecurityIdentity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordServiceSseCancellationTest {

    static final String TOPIC_ID = "topic-id";
    static final String TOPIC_NAME = "topic-name";
    static final TopicPartition PARTITION = new TopicPartition(TOPIC_NAME, 0);
    static final long END_OFFSET = 3;
    static final int MATCH_LIMIT = 100;
    static final Duration WAIT = Duration.ofSeconds(5);

    ControlledConsumer consumer;
    CountDownLatch dataSent;
    CountDownLatch predicateOnOffset1Entered;
    CountDownLatch releasePredicate;
    AtomicBoolean closed;
    AtomicInteger closedChecks;
    AtomicInteger evaluatedOffset1;
    AtomicInteger evaluatedOffset2;
    List<String> receivedEvents;
    RecordService service;

    static class ControlledConsumer extends MockConsumer<RecordData, RecordData> {

        final AtomicInteger pollCalls = new AtomicInteger();

        ControlledConsumer() {
            super("earliest");
        }

        @Override
        public synchronized ConsumerRecords<RecordData, RecordData> poll(Duration timeout) {
            int call = pollCalls.incrementAndGet();

            if (call > 2) {
                throw new IllegalStateException("Third poll after client cancellation");
            }

            ConsumerRecords<RecordData, RecordData> records = super.poll(timeout);

            if (call == 1) {
                addRecord(record(1));
                addRecord(record(2));
            }

            return records;
        }
    }

    static ConsumerRecord<RecordData, RecordData> record(long offset) {
        return new ConsumerRecord<>(TOPIC_NAME, 0, offset,
            new RecordData("key-" + offset), new RecordData("value-" + offset));
    }

    @BeforeEach
    void setup() {
        consumer = new ControlledConsumer();
        consumer.updatePartitions(TOPIC_NAME, List.of(new PartitionInfo(TOPIC_NAME, 0, null, null, null)));
        consumer.updateBeginningOffsets(Map.of(PARTITION, 0L));
        consumer.updateEndOffsets(Map.of(PARTITION, END_OFFSET));
        consumer.assign(List.of(PARTITION));
        consumer.seek(PARTITION, 0);
        consumer.addRecord(record(0));

        dataSent = new CountDownLatch(1);
        predicateOnOffset1Entered = new CountDownLatch(1);
        releasePredicate = new CountDownLatch(1);
        closed = new AtomicBoolean();
        closedChecks = new AtomicInteger();
        evaluatedOffset1 = new AtomicInteger();
        evaluatedOffset2 = new AtomicInteger();
        receivedEvents = new CopyOnWriteArrayList<>();

        service = new RecordService();
        service.logger = mock(Logger.class);
        service.config = mock(Config.class);
        when(service.config.getOptionalValue(RecordService.STREAM_TIMEOUT_CONFIG, Duration.class))
            .thenReturn(Optional.of(Duration.ofSeconds(2)));

        service.securityIdentity = mock(SecurityIdentity.class);
        when(service.securityIdentity.getPrincipal()).thenReturn(() -> "test-user");

        service.kafkaContext = mock(KafkaContext.class);
        when(service.kafkaContext.clusterId()).thenReturn("test-cluster");

        service.streamConsumer = consumer;

        service.topicService = mock(TopicDescribeService.class);
        when(service.topicService.topicNameForId(TOPIC_ID))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(TOPIC_NAME)));
    }

    Predicate<ConsumerRecord<RecordData, RecordData>> predicate() {
        return rec -> {
            long offset = rec.offset();

            if (offset == 0) {
                return true;
            }

            if (offset == 1) {
                evaluatedOffset1.incrementAndGet();
                predicateOnOffset1Entered.countDown();
                try {
                    if (!releasePredicate.await(WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IllegalStateException("Predicate on offset 1 was not released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while predicate on offset 1 was blocked", e);
                }
            } else {
                evaluatedOffset2.incrementAndGet();
            }

            return false;
        };
    }

    SseEventSink sink() {
        SseEventSink sink = mock(SseEventSink.class);
        when(sink.isClosed()).thenAnswer(invocation -> {
            boolean isClosed = closed.get();
            if (isClosed) {
                closedChecks.incrementAndGet();
            }
            return isClosed;
        });
        doAnswer(invocation -> {
            OutboundSseEvent event = invocation.getArgument(0);
            receivedEvents.add(event.getName());
            if (event.getName() == null) {
                dataSent.countDown();
            }
            return CompletableFuture.completedFuture(null);
        }).when(sink).send(any(OutboundSseEvent.class));
        doAnswer(invocation -> {
            closed.set(true);
            return null;
        }).when(sink).close();
        return sink;
    }

    Sse sse() {
        Sse sse = mock(Sse.class);
        when(sse.newEventBuilder()).thenAnswer(invocation -> {
            AtomicReference<String> name = new AtomicReference<>();
            OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, Answers.RETURNS_SELF);
            when(builder.name(anyString())).thenAnswer(builderInvocation -> {
                name.set(builderInvocation.getArgument(0));
                return builder;
            });
            when(builder.build()).thenAnswer(builderInvocation -> {
                OutboundSseEvent event = mock(OutboundSseEvent.class);
                when(event.getName()).thenReturn(name.get());
                return event;
            });
            return builder;
        });
        return sse;
    }

    RecordFilterParams params(Predicate<ConsumerRecord<RecordData, RecordData>> predicate) {
        RecordFilterParams params = mock(RecordFilterParams.class);
        when(params.getOffset()).thenReturn(0L);
        when(params.getLimit()).thenReturn(MATCH_LIMIT);
        doReturn(List.of(predicate)).when(params).getPredicates(ConsumerRecord.class);
        return params;
    }

    @Test
    void testCancellationWhileStreamingDoesNotSendDone() throws Exception {
        SseEventSink sink = sink();
        Sse sse = sse();
        RecordFilterParams params = params(predicate());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> completion = executor.submit(() -> service.consumeRecords(
            TOPIC_ID, List.of("value"), params, sink, sse));

        try {
            assertThat("Data event was not sent", dataSent.await(WAIT.toMillis(), TimeUnit.MILLISECONDS), is(true));
            assertThat("Predicate did not block on offset 1",
                predicateOnOffset1Entered.await(WAIT.toMillis(), TimeUnit.MILLISECONDS), is(true));
            assertThat("Offset 2 evaluated before cancellation", evaluatedOffset2.get(), is(0));

            closed.set(true);
        } finally {
            releasePredicate.countDown();
            try {
                completion.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                executor.shutdownNow();
                assertThat("Executor did not terminate",
                    executor.awaitTermination(WAIT.toMillis(), TimeUnit.MILLISECONDS), is(true));
            }
        }

        assertThat(consumer.pollCalls.get(), is(2));
        assertThat(receivedEvents.stream().filter(Objects::isNull).count(), is(1L));
        assertThat(evaluatedOffset1.get(), is(1));
        assertThat(evaluatedOffset2.get(), is(0));
        assertThat(consumer.endOffsets(List.of(PARTITION)).get(PARTITION), is(END_OFFSET));
        assertThat("Closed sink was observed only by the terminal-event guard",
            closedChecks.get(), is(greaterThanOrEqualTo(2)));
        assertThat(service.activeStreams(), is(0));
        assertThat(receivedEvents, not(hasItem("done")));
    }
}
