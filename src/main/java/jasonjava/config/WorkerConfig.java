package jasonjava.config;

import jasonjava.handler.TaskType;

import com.google.pubsub.v1.ProjectSubscriptionName;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public record WorkerConfig(
        ProjectSubscriptionName subscription,
        Map<TaskType, Path> scripts,
        Duration scriptTimeout
) {
    private static final String PROJECT_ID = "jasonapp-xm0830";
    private static final String SUBSCRIPTION_ID = "asphalt_legend_windows";
    private static final Path RESET_SCRIPT = script("reset.ps1");
    private static final Path REFILL_ADS_SCRIPT = script("refill-ads.ps1");
    private static final Path START_GAME_SCRIPT = script("start-game.ps1");
    private static final Duration SCRIPT_TIMEOUT = Duration.ofMinutes(10);

    public static WorkerConfig windowsDefaults() {
        return new WorkerConfig(
                ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID),
                Map.of(
                        TaskType.RESET, RESET_SCRIPT,
                        TaskType.REFILL_ADS, REFILL_ADS_SCRIPT,
                        TaskType.START_GAME, START_GAME_SCRIPT
                ),
                SCRIPT_TIMEOUT
        );
    }

    private static Path script(String fileName) {
        return Path.of("scripts", fileName).toAbsolutePath().normalize();
    }
}
