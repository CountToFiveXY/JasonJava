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
        assertTrue(config.scripts().values().stream().allMatch(path -> path.toFile().isFile()));
        assertEquals(Duration.ofMinutes(10), config.scriptTimeout());
    }
}
