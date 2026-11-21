package jasonjava.config;

import com.google.pubsub.v1.ProjectSubscriptionName;
import jasonjava.handler.TaskType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerConfigTest {
    @Test
    void providesTheWindowsWorkerConfiguration() {
        WorkerConfig config = WorkerConfig.windowsDefaults();

        assertEquals(
                ProjectSubscriptionName.of("jasonapp-xm0830", "asphalt_legend_windows"),
                config.subscription()
        );
        assertEquals(
                Path.of("scripts", "reset.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.RESET)
        );
        assertEquals(
                Path.of("scripts", "refill-ads.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.REFILL_ADS)
        );
        assertEquals(
                Path.of("scripts", "start-game.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.START_GAME)
        );
        assertEquals(
                Path.of("scripts", "close-game.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.CLOSE_GAME)
        );
        assertEquals(
                Path.of("scripts", "window.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.WINDOW)
        );
        assertEquals(
                Path.of("scripts", "mute.ps1").toAbsolutePath().normalize(),
                config.scripts().get(TaskType.MUTE)
        );
        assertTrue(config.scripts().values().stream().allMatch(path -> path.toFile().isFile()));
        assertEquals(Duration.ofMinutes(10), config.scriptTimeout());
    }
}
