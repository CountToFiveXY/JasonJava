package jasonjava.handler;

import java.util.Objects;

public record TaskCommand(TaskType type, int repeat) {
    public TaskCommand {
        Objects.requireNonNull(type, "type");
        if (type == TaskType.RESET && repeat != 1) {
            throw new IllegalArgumentException("RESET does not support repeat");
        }
        if (type == TaskType.REFILL_ADS && (repeat < 1 || repeat > 99)) {
            throw new IllegalArgumentException("REFILL_ADS repeat must be between 1 and 99");
        }
    }

    public static TaskCommand reset() {
        return new TaskCommand(TaskType.RESET, 1);
    }
}
