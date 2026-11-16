package jasonjava.execution;

import java.nio.file.Path;
import java.time.Duration;

public interface ScriptExecutor {
    void execute(Path script, Duration timeout) throws Exception;
}
