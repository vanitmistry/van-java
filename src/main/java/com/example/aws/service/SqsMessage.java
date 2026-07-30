package com.example.aws.service;

public record SqsMessage(byte[] body, String receiptHandle, String messageId) {
}
