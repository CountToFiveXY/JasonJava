package jasonjava.polling;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import jasonjava.handler.TaskCommand;
import jasonjava.handler.TaskHandler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TaskMessageReceiver implements MessageReceiver {
    private static final Logger LOGGER = Logger.getLogger(TaskMessageReceiver.class.getName());

    private final TaskMessageParser parser;
    private final TaskHandler handler;

    public TaskMessageReceiver(TaskMessageParser parser, TaskHandler handler) {
        this.parser = parser;
        this.handler = handler;
    }

    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
        final TaskCommand task;
        try {
            task = parser.parse(message.getData().toByteArray());
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Discarding invalid Pub/Sub message " + message.getMessageId(),
                    exception
            );
            consumer.ack();
            return;
        }

        try {
            LOGGER.info(() -> "Running " + task.type() + " from message " + message.getMessageId());
            handler.handle(task);
            consumer.ack();
            LOGGER.info(() -> "Completed " + task.type() + " from message " + message.getMessageId());
        } catch (Exception exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Task " + task.type() + " failed; Pub/Sub may redeliver message " + message.getMessageId(),
                    exception
            );
            consumer.nack();
        }
    }
}
