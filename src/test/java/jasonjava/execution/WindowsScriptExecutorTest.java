package jasonjava.execution;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsScriptExecutorTest {
    @Test
    void buildsCommandsWithoutInterpretingMessageData() {
        Path script = Path.of("C:\\Jason Scripts\\reset.ps1");
        assertEquals(
                List.of(
                        "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString()
                ),
                WindowsScriptExecutor.commandFor(script)
        );
        assertEquals(
                List.of(
                        "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                        "-Repeat", "12"
                ),
                WindowsScriptExecutor.commandFor(script, List.of("-Repeat", "12"))
        );
        assertEquals(
                List.of("C:\\Tools\\refill.exe"),
                WindowsScriptExecutor.commandFor(Path.of("C:\\Tools\\refill.exe"))
        );
    }

    @Test
    void rejectsUnrecognizedFileTypes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> WindowsScriptExecutor.commandFor(Path.of("C:\\Tools\\task.js"))
        );
    }
}
