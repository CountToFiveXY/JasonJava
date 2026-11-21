package jasonjava.polling;

import jasonjava.handler.TaskCommand;
import jasonjava.handler.TaskType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class TaskMessageParser {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public TaskCommand parse(byte[] data) throws IOException {
        TaskMessage message = objectMapper.readValue(data, TaskMessage.class);
        if (message.type() == null) {
            throw new IOException("Task message is missing type");
        }
        return switch (message.type()) {
            case RESET -> withoutRepeat(message, TaskCommand.reset());
            case START_GAME -> withoutRepeat(message, TaskCommand.startGame());
            case CLOSE_GAME -> withoutRepeat(message, TaskCommand.closeGame());
            case WINDOW -> withoutRepeat(message, TaskCommand.window());
            case MUTE -> withoutRepeat(message, TaskCommand.mute());
            case REFILL_ADS -> refillAds(message);
        };
    }

    private TaskCommand withoutRepeat(TaskMessage message, TaskCommand command) throws IOException {
        if (message.repeat() != null) {
            throw new IOException(message.type() + " does not support repeat");
        }
        return command;
    }

    private TaskCommand refillAds(TaskMessage message) throws IOException {
        int repeat = message.repeat() == null ? 8 : message.repeat();
        if (repeat < 1 || repeat > 99) {
            throw new IOException("REFILL_ADS repeat must be between 1 and 99");
        }
        return new TaskCommand(TaskType.REFILL_ADS, repeat);
    }

    private record TaskMessage(TaskType type, Integer repeat) {
        @JsonCreator
        private TaskMessage(
                @JsonProperty(value = "type", required = true) TaskType type,
                @JsonProperty("repeat") Integer repeat
        ) {
            this.type = type;
            this.repeat = repeat;
        }
    }
}
