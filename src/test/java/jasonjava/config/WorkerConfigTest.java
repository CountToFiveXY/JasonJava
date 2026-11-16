package jasonjava.config;

import jasonjava.handler.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkerConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void requiresResetToBeAPowerShellScript() throws Exception {
        Path reset = Files.createFile(temporaryDirectory.resolve("reset.ps1"));
        Path refill = Files.createFile(temporaryDirectory.resolve("refill.cmd"));

        WorkerConfig config = WorkerConfig.fromEnvironment(environment(reset, refill));

        assertEquals(reset, config.scripts().get(TaskType.RESET));
        assertEquals(refill, config.scripts().get(TaskType.REFILL_ADS));
    }

    @Test
    void rejectsANonPowerShellResetHandler() throws Exception {
        Path reset = Files.createFile(temporaryDirectory.resolve("reset.cmd"));
        Path refill = Files.createFile(temporaryDirectory.resolve("refill.cmd"));

        assertThrows(
                IllegalArgumentException.class,
                () -> WorkerConfig.fromEnvironment(environment(reset, refill))
        );
    }

    private Map<String, String> environment(Path reset, Path refill) {
        Map<String, String> environment = new HashMap<>();
        environment.put("JASON_GCP_PROJECT", "test-project");
        environment.put("JASON_RESET_SCRIPT", reset.toString());
        environment.put("JASON_REFILL_ADS_SCRIPT", refill.toString());
        return environment;
    }
}
