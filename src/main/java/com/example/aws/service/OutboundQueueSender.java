package com.example.aws.service;

import java.util.Base64;

import org.springframework.stereotype.Service;

import com.example.aws.config.SqsProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class OutboundQueueSender {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public OutboundQueueSender(SqsClient sqsClient, SqsProperties properties) {
        this.sqsClient = sqsClient;
        this.queueUrl = sqsClient.getQueueUrl(
                GetQueueUrlRequest.builder().queueName(properties.getOutQueueName()).build())
                .queueUrl();
    }

    public String sendMessage(byte[] payload) {
        String encodedBody = Base64.getEncoder().encodeToString(payload);
        return sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(encodedBody)
                        .build())
                .messageId();
    }
}
