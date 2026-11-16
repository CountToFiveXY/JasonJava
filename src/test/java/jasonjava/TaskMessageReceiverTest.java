package jasonjava;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaskMessageReceiverTest {
    @Test
    void acknowledgesOnlyAfterSuccessfulExecution() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"RESET\"}"), consumer);

        assertEquals(Path.of("reset.ps1"), executor.executed);
        assertEquals(1, consumer.acks);
        assertEquals(0, consumer.nacks);
    }

    @Test
    void negativelyAcknowledgesScriptFailure() {
        RecordingExecutor executor = new RecordingExecutor(true);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"RESET\"}"), consumer);

        assertEquals(0, consumer.acks);
        assertEquals(1, consumer.nacks);
    }

    @Test
    void discardsInvalidMessagesWithoutExecutingThem() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"NOT_ALLOWED\"}"), consumer);

        assertNull(executor.executed);
        assertEquals(1, consumer.acks);
        assertEquals(0, consumer.nacks);
    }

    private TaskMessageReceiver receiver(RecordingExecutor executor) {
        TaskHandler handler = new TaskHandler(
                Map.of(
                        TaskType.RESET, Path.of("reset.ps1"),
                        TaskType.REFILL_ADS, Path.of("refill.ps1")
                ),
                Duration.ofSeconds(30),
                executor
        );
        return new TaskMessageReceiver(new TaskMessageParser(), handler);
    }

    private PubsubMessage message(String json) {
        return PubsubMessage.newBuilder()
                .setMessageId("test-message")
                .setData(ByteString.copyFromUtf8(json))
                .build();
    }

    private static final class RecordingExecutor implements ScriptExecutor {
        private final boolean fail;
        private Path executed;

        private RecordingExecutor(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void execute(Path script, Duration timeout) throws IOException {
            executed = script;
            if (fail) {
                throw new IOException("test failure");
            }
        }
    }

    private static final class RecordingConsumer implements AckReplyConsumer {
        private int acks;
        private int nacks;

        @Override
        public void ack() {
            acks++;
        }

        @Override
        public void nack() {
            nacks++;
        }
    }
}
