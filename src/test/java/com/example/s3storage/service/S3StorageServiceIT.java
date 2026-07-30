package com.example.s3storage.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import com.example.s3storage.testsupport.AbstractLocalStackIT;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3StorageServiceIT extends AbstractLocalStackIT {

    private static final String BUCKET = "s3-storage-service-bucket";

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private S3Client s3Client;

    @BeforeAll
    static void createBucket() {
        try (S3Client bootstrapClient = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(Service.S3))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .forcePathStyle(true)
                .build()) {
            bootstrapClient.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @Test
    void uploadCreatesObjectInBucket() {
        String key = "upload-check.txt";
        byte[] content = "created via putObject".getBytes(StandardCharsets.UTF_8);

        s3StorageService.putObject(BUCKET, key, content);

        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(key).build());
        assertThat(head.contentLength()).isEqualTo((long) content.length);
    }

    @Test
    void downloadReturnsPreviouslyStoredContent() {
        String key = "download-check.txt";
        byte[] content = "pre-existing content".getBytes(StandardCharsets.UTF_8);
        s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(),
                RequestBody.fromBytes(content));

        byte[] downloaded = s3StorageService.getObject(BUCKET, key);

        assertThat(downloaded).isEqualTo(content);
    }

    @Test
    void putThenGetRoundTripReturnsByteIdenticalContent() {
        String key = "round-trip.txt";
        byte[] content = "round trip payload".getBytes(StandardCharsets.UTF_8);

        s3StorageService.putObject(BUCKET, key, content);
        byte[] downloaded = s3StorageService.getObject(BUCKET, key);

        assertThat(downloaded).isEqualTo(content);
    }
}
