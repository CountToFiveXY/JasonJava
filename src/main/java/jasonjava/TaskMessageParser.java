package jasonjava;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class TaskMessageParser {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    TaskType parse(byte[] data) throws IOException {
        TaskMessage message = objectMapper.readValue(data, TaskMessage.class);
        if (message.type() == null) {
            throw new IOException("Task message is missing type");
        }
        return message.type();
    }

    private record TaskMessage(TaskType type) {
        @JsonCreator
        private TaskMessage(@JsonProperty(value = "type", required = true) TaskType type) {
            this.type = type;
        }
    }
}
