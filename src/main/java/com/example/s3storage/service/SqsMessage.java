package com.example.s3storage.service;

public record SqsMessage(byte[] body, String receiptHandle, String messageId) {
}
