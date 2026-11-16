package jasonjava;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

final class TaskHandler {
    private final Map<TaskType, Path> scripts;
    private final Duration timeout;
    private final ScriptExecutor executor;

    TaskHandler(Map<TaskType, Path> scripts, Duration timeout, ScriptExecutor executor) {
        this.scripts = scripts;
        this.timeout = timeout;
        this.executor = executor;
    }

    synchronized void handle(TaskType type) throws Exception {
        Path script = scripts.get(type);
        if (script == null) {
            throw new IllegalArgumentException("No script configured for task " + type);
        }
        executor.execute(script, timeout);
    }
}
