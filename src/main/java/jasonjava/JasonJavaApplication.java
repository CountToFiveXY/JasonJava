package jasonjava;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController;
import com.google.cloud.pubsub.v1.Subscriber;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JasonJavaApplication {
    private static final Logger LOGGER = Logger.getLogger(JasonJavaApplication.class.getName());

    private JasonJavaApplication() {}

    public static void main(String[] args) {
        try {
            WorkerConfig config = WorkerConfig.fromEnvironment(System.getenv());
            config.scripts().values().forEach(WindowsScriptExecutor::commandFor);
            TaskHandler handler = new TaskHandler(
                    config.scripts(),
                    config.scriptTimeout(),
                    new WindowsScriptExecutor()
            );
            TaskMessageReceiver receiver = new TaskMessageReceiver(new TaskMessageParser(), handler);
            FlowControlSettings flowControl = FlowControlSettings.newBuilder()
                    .setMaxOutstandingElementCount(1L)
                    .setLimitExceededBehavior(FlowController.LimitExceededBehavior.Block)
                    .build();
            Subscriber subscriber = Subscriber.newBuilder(config.subscription(), receiver)
                    .setFlowControlSettings(flowControl)
                    .setParallelPullCount(1)
                    .build();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(subscriber), "shutdown"));
            subscriber.startAsync().awaitRunning();
            LOGGER.info(() -> "JasonJava is listening on " + config.subscription());
            subscriber.awaitTerminated();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "JasonJava stopped", exception);
            System.exit(1);
        }
    }

    private static void stop(Subscriber subscriber) {
        LOGGER.info("Stopping JasonJava");
        subscriber.stopAsync();
        try {
            subscriber.awaitTerminated(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Subscriber did not stop cleanly", exception);
        }
    }
}
