package jasonjava.handler;

import java.util.Objects;

public record TaskCommand(TaskType type, int repeat) {
    public TaskCommand {
        Objects.requireNonNull(type, "type");
        if (type != TaskType.REFILL_ADS && repeat != 1) {
            throw new IllegalArgumentException(type + " does not support repeat");
        }
        if (type == TaskType.REFILL_ADS && (repeat < 1 || repeat > 99)) {
            throw new IllegalArgumentException("REFILL_ADS repeat must be between 1 and 99");
        }
    }

    public static TaskCommand reset() {
        return new TaskCommand(TaskType.RESET, 1);
    }

    public static TaskCommand startGame() {
        return new TaskCommand(TaskType.START_GAME, 1);
    }

    public static TaskCommand closeGame() {
        return new TaskCommand(TaskType.CLOSE_GAME, 1);
    }

    public static TaskCommand window() {
        return new TaskCommand(TaskType.WINDOW, 1);
    }

    public static TaskCommand mute() {
        return new TaskCommand(TaskType.MUTE, 1);
    }
}
