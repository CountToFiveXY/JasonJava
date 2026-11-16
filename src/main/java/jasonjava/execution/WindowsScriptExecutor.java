package jasonjava.execution;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WindowsScriptExecutor implements ScriptExecutor {
    @Override
    public void execute(
            Path script,
            List<String> arguments,
            Duration timeout
    ) throws IOException, InterruptedException {
        List<String> command = commandFor(script, arguments);
        Process process = new ProcessBuilder(command)
                .directory(script.getParent().toFile())
                .inheritIO()
                .start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            throw new IOException("Script timed out after " + timeout.toSeconds() + " seconds");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Script exited with code " + process.exitValue());
        }
    }

    public static List<String> commandFor(Path script) {
        return commandFor(script, List.of());
    }

    public static List<String> commandFor(Path script, List<String> arguments) {
        String file = script.toString();
        String lowerName = script.getFileName().toString().toLowerCase(Locale.ROOT);
        List<String> command = new ArrayList<>();
        if (lowerName.endsWith(".ps1")) {
            command.addAll(List.of(
                    "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-File", file
            ));
        } else if (lowerName.endsWith(".bat") || lowerName.endsWith(".cmd")) {
            command.addAll(List.of("cmd.exe", "/d", "/s", "/c", "\"" + file + "\""));
        } else if (lowerName.endsWith(".exe")) {
            command.add(file);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported script type for " + script + "; use .ps1, .bat, .cmd, or .exe"
            );
        }
        command.addAll(arguments);
        return List.copyOf(command);
    }
}
