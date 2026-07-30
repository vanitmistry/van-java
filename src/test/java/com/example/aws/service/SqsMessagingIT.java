package com.example.aws.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.aws.testsupport.AbstractLocalStackIT;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class SqsMessagingIT extends AbstractLocalStackIT {

    private static final String IN_QUEUE = "in-queue";

    @Autowired
    private OutboundQueueSender outboundQueueSender;

    @Autowired
    private InboundQueueReceiver inboundQueueReceiver;

    @Autowired
    private SqsClient sqsClient;

    @Test
    void sendMessageReturnsMessageId() {
        String messageId = outboundQueueSender.sendMessage("hello out-queue".getBytes(StandardCharsets.UTF_8));

        assertThat(messageId).isNotBlank();
    }

    @Test
    void receiveReturnsMessageWithoutDeletingIt() {
        byte[] content = "hello in-queue".getBytes(StandardCharsets.UTF_8);
        putRawMessage(content);

        Optional<SqsMessage> received = inboundQueueReceiver.receiveMessage();

        assertThat(received).isPresent();
        assertThat(received.get().body()).isEqualTo(content);
        assertThat(received.get().receiptHandle()).isNotBlank();
        assertThat(received.get().messageId()).isNotBlank();
        assertThat(messagesNotVisibleOnInQueue()).isGreaterThanOrEqualTo(1);

        inboundQueueReceiver.deleteMessage(received.get().receiptHandle());
    }

    @Test
    void receiveOnEmptyQueueReturnsEmptyResult() {
        drainInQueue();

        Optional<SqsMessage> received = inboundQueueReceiver.receiveMessage();

        assertThat(received).isEmpty();
    }

    @Test
    void deleteMessageRemovesItFromQueue() {
        putRawMessage("to be deleted".getBytes(StandardCharsets.UTF_8));

        Optional<SqsMessage> received = inboundQueueReceiver.receiveMessage();
        assertThat(received).isPresent();
        inboundQueueReceiver.deleteMessage(received.get().receiptHandle());

        assertThat(inboundQueueReceiver.receiveMessage()).isEmpty();
    }

    @Test
    void sendToOutQueueAndReceiveDeleteOnInQueueAreIndependent() {
        String messageId = outboundQueueSender.sendMessage("independent out message".getBytes(StandardCharsets.UTF_8));
        assertThat(messageId).isNotBlank();

        putRawMessage("independent in message".getBytes(StandardCharsets.UTF_8));
        Optional<SqsMessage> received = inboundQueueReceiver.receiveMessage();
        assertThat(received).isPresent();
        inboundQueueReceiver.deleteMessage(received.get().receiptHandle());

        assertThat(inboundQueueReceiver.receiveMessage()).isEmpty();
    }

    private void putRawMessage(byte[] content) {
        String queueUrl = sqsClient.getQueueUrl(
                        GetQueueUrlRequest.builder().queueName(IN_QUEUE).build())
                .queueUrl();
        String encodedBody = Base64.getEncoder().encodeToString(content);
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(encodedBody).build());
    }

    private int messagesNotVisibleOnInQueue() {
        String queueUrl = sqsClient.getQueueUrl(
                        GetQueueUrlRequest.builder().queueName(IN_QUEUE).build())
                .queueUrl();
        String value = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
                        .build())
                .attributes()
                .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);
        return Integer.parseInt(value);
    }

    private void drainInQueue() {
        Optional<SqsMessage> message;
        while ((message = inboundQueueReceiver.receiveMessage()).isPresent()) {
            inboundQueueReceiver.deleteMessage(message.get().receiptHandle());
        }
    }
}
