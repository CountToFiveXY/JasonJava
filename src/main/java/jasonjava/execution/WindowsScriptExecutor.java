package jasonjava.execution;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WindowsScriptExecutor implements ScriptExecutor {
    @Override
    public void execute(Path script, Duration timeout) throws IOException, InterruptedException {
        List<String> command = commandFor(script);
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
        String file = script.toString();
        String lowerName = script.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".ps1")) {
            return List.of(
                    "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-File", file
            );
        }
        if (lowerName.endsWith(".bat") || lowerName.endsWith(".cmd")) {
            return List.of("cmd.exe", "/d", "/s", "/c", "\"" + file + "\"");
        }
        if (lowerName.endsWith(".exe")) {
            return List.of(file);
        }
        throw new IllegalArgumentException(
                "Unsupported script type for " + script + "; use .ps1, .bat, .cmd, or .exe"
        );
    }
}
