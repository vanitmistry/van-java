package com.example.aws.service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.aws.config.SqsProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Service
public class InboundQueueReceiver {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public InboundQueueReceiver(SqsClient sqsClient, SqsProperties properties) {
        this.sqsClient = sqsClient;
        this.queueUrl = sqsClient.getQueueUrl(
                GetQueueUrlRequest.builder().queueName(properties.getInQueueName()).build())
                .queueUrl();
    }

    public Optional<SqsMessage> receiveMessage() {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .build())
                .messages();

        if (messages.isEmpty()) {
            return Optional.empty();
        }

        Message message = messages.get(0);
        byte[] body = Base64.getDecoder().decode(message.body());
        return Optional.of(new SqsMessage(body, message.receiptHandle(), message.messageId()));
    }

    public void deleteMessage(String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build());
    }
}
