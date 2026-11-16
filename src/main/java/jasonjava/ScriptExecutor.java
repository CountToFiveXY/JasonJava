package jasonjava;

import java.nio.file.Path;
import java.time.Duration;

interface ScriptExecutor {
    void execute(Path script, Duration timeout) throws Exception;
}
