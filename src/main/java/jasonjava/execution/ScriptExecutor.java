package jasonjava.execution;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface ScriptExecutor {
    void execute(Path script, List<String> arguments, Duration timeout) throws Exception;
}
