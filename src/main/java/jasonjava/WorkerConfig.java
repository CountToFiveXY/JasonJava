package jasonjava;

import com.google.pubsub.v1.ProjectSubscriptionName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

record WorkerConfig(
        ProjectSubscriptionName subscription,
        Map<TaskType, Path> scripts,
        Duration scriptTimeout
) {
    private static final String DEFAULT_SUBSCRIPTION = "asphalt_legend_windows";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    static WorkerConfig fromEnvironment(Map<String, String> environment) {
        String project = required(environment, "JASON_GCP_PROJECT");
        String subscription = environment.getOrDefault(
                "JASON_PUBSUB_SUBSCRIPTION",
                DEFAULT_SUBSCRIPTION
        ).trim();
        if (subscription.isEmpty()) {
            throw new IllegalArgumentException("JASON_PUBSUB_SUBSCRIPTION must not be blank");
        }

        EnumMap<TaskType, Path> scripts = new EnumMap<>(TaskType.class);
        scripts.put(TaskType.RESET, scriptPath(environment, "JASON_RESET_SCRIPT"));
        scripts.put(TaskType.REFILL_ADS, scriptPath(environment, "JASON_REFILL_ADS_SCRIPT"));

        Duration timeout = DEFAULT_TIMEOUT;
        String timeoutSeconds = environment.get("JASON_SCRIPT_TIMEOUT_SECONDS");
        if (timeoutSeconds != null) {
            try {
                timeout = Duration.ofSeconds(Long.parseLong(timeoutSeconds));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "JASON_SCRIPT_TIMEOUT_SECONDS must be a whole number",
                        exception
                );
            }
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("JASON_SCRIPT_TIMEOUT_SECONDS must be positive");
            }
        }

        return new WorkerConfig(
                ProjectSubscriptionName.of(project, subscription),
                Map.copyOf(scripts),
                timeout
        );
    }

    private static Path scriptPath(Map<String, String> environment, String name) {
        Path path = Path.of(required(environment, name)).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(name + " is not a file: " + path);
        }
        return path;
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
