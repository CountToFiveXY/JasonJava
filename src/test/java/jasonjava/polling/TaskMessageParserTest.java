package jasonjava.polling;

import jasonjava.handler.TaskType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskMessageParserTest {
    private final TaskMessageParser parser = new TaskMessageParser();

    @Test
    void parsesTheJasonAppContract() throws Exception {
        assertEquals(TaskType.RESET, parse("{\"type\":\"RESET\"}"));
        assertEquals(TaskType.REFILL_ADS, parse("{\"type\":\"REFILL_ADS\"}"));
    }

    @Test
    void rejectsUnknownTasksAndFields() {
        assertThrows(IOException.class, () -> parse("{\"type\":\"DELETE_ALL\"}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"RESET\",\"command\":\"evil\"}"));
        assertThrows(IOException.class, () -> parse("{}"));
    }

    private TaskType parse(String json) throws Exception {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
