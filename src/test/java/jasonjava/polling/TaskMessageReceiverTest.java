package jasonjava.polling;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import jasonjava.execution.ScriptExecutor;
import jasonjava.handler.TaskHandler;
import jasonjava.handler.TaskType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
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
        assertEquals(List.of(), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
        assertEquals(0, consumer.nacks);
    }

    @Test
    void passesRefillRepeatToOneScriptProcess() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(
                message("{\"type\":\"REFILL_ADS\",\"repeat\":12}"),
                consumer
        );

        assertEquals(Path.of("refill.ps1"), executor.executed);
        assertEquals(List.of("-Repeat", "12"), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
    }

    @Test
    void runsStartGameScriptOnceWithoutArguments() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"START_GAME\"}"), consumer);

        assertEquals(Path.of("start-game.ps1"), executor.executed);
        assertEquals(List.of(), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
    }

    @Test
    void runsCloseGameScriptOnceWithoutArguments() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"CLOSE_GAME\"}"), consumer);

        assertEquals(Path.of("close-game.ps1"), executor.executed);
        assertEquals(List.of(), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
    }

    @Test
    void runsWindowScriptOnceWithoutArguments() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"WINDOW\"}"), consumer);

        assertEquals(Path.of("window.ps1"), executor.executed);
        assertEquals(List.of(), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
    }

    @Test
    void runsMuteScriptOnceWithoutArguments() {
        RecordingExecutor executor = new RecordingExecutor(false);
        RecordingConsumer consumer = new RecordingConsumer();

        receiver(executor).receiveMessage(message("{\"type\":\"MUTE\"}"), consumer);

        assertEquals(Path.of("mute.ps1"), executor.executed);
        assertEquals(List.of(), executor.arguments);
        assertEquals(1, executor.executionCount);
        assertEquals(1, consumer.acks);
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
                        TaskType.REFILL_ADS, Path.of("refill.ps1"),
                        TaskType.START_GAME, Path.of("start-game.ps1"),
                        TaskType.CLOSE_GAME, Path.of("close-game.ps1"),
                        TaskType.WINDOW, Path.of("window.ps1"),
                        TaskType.MUTE, Path.of("mute.ps1")
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
        private List<String> arguments;
        private int executionCount;

        private RecordingExecutor(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void execute(Path script, List<String> arguments, Duration timeout) throws IOException {
            executed = script;
            this.arguments = arguments;
            executionCount++;
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
