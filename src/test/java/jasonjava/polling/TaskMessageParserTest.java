package jasonjava.polling;

import jasonjava.handler.TaskCommand;
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
        assertEquals(TaskCommand.reset(), parse("{\"type\":\"RESET\"}"));
        assertEquals(
                new TaskCommand(TaskType.REFILL_ADS, 12),
                parse("{\"type\":\"REFILL_ADS\",\"repeat\":12}")
        );
        assertEquals(
                new TaskCommand(TaskType.REFILL_ADS, 8),
                parse("{\"type\":\"REFILL_ADS\"}")
        );
        assertEquals(TaskCommand.startGame(), parse("{\"type\":\"START_GAME\"}"));
        assertEquals(TaskCommand.closeGame(), parse("{\"type\":\"CLOSE_GAME\"}"));
        assertEquals(TaskCommand.window(), parse("{\"type\":\"WINDOW\"}"));
    }

    @Test
    void rejectsUnknownTasksAndFields() {
        assertThrows(IOException.class, () -> parse("{\"type\":\"DELETE_ALL\"}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"RESET\",\"command\":\"evil\"}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"RESET\",\"repeat\":8}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"START_GAME\",\"repeat\":8}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"CLOSE_GAME\",\"repeat\":8}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"WINDOW\",\"repeat\":8}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"REFILL_ADS\",\"repeat\":0}"));
        assertThrows(IOException.class, () -> parse("{\"type\":\"REFILL_ADS\",\"repeat\":100}"));
        assertThrows(IOException.class, () -> parse("{}"));
    }

    private TaskCommand parse(String json) throws Exception {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
