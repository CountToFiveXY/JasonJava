package jasonjava.handler;

import jasonjava.execution.ScriptExecutor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class TaskHandler {
    private final Map<TaskType, Path> scripts;
    private final Duration timeout;
    private final ScriptExecutor executor;

    public TaskHandler(Map<TaskType, Path> scripts, Duration timeout, ScriptExecutor executor) {
        this.scripts = scripts;
        this.timeout = timeout;
        this.executor = executor;
    }

    public synchronized void handle(TaskCommand task) throws Exception {
        Path script = scripts.get(task.type());
        if (script == null) {
            throw new IllegalArgumentException("No script configured for task " + task.type());
        }
        List<String> arguments = task.type() == TaskType.REFILL_ADS
                ? List.of("-Repeat", Integer.toString(task.repeat()))
                : List.of();
        executor.execute(script, arguments, timeout);
    }
}
