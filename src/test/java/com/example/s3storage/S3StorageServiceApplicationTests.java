package com.example.s3storage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class S3StorageServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the default (non-local) profile wires the S3Client bean
        // without requiring LocalStack or real AWS credentials.
    }
}
