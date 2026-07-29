## ADDED Requirements

### Requirement: Upload an object to S3
The system SHALL provide a service method that uploads (writes) a byte payload to a configured S3 bucket under a given key, using AWS SDK v2.

#### Scenario: Successful upload
- **WHEN** `S3StorageService.putObject` is called with a bucket, a key, and byte content
- **THEN** an object with that key and content exists in the bucket afterward

### Requirement: Download an object from S3
The system SHALL provide a service method that downloads (reads) the byte content of an existing object from a configured S3 bucket by key.

#### Scenario: Successful download
- **WHEN** `S3StorageService.getObject` is called with a bucket and a key for an object that exists
- **THEN** the method returns the exact byte content previously stored under that key

#### Scenario: Round-trip put then get
- **WHEN** an object is uploaded via `putObject` and then immediately retrieved via `getObject` using the same bucket and key
- **THEN** the retrieved content is byte-for-byte identical to the uploaded content

### Requirement: Environment-specific S3 endpoint configuration
The system SHALL determine the S3 endpoint and credentials from Spring configuration profiles, without any code branching in the service layer.

#### Scenario: Default profile targets real AWS
- **WHEN** the application runs with no profile or a non-local profile active
- **THEN** the `S3Client` bean uses the AWS default credential provider chain and no endpoint override, targeting real AWS S3

#### Scenario: Local/test profile targets LocalStack
- **WHEN** the application or test runs with the `local` (or test) profile active
- **THEN** the `S3Client` bean uses a static dummy credentials provider and an endpoint override pointing at the configured LocalStack endpoint

### Requirement: Hermetic integration test suite via LocalStack
The system SHALL include an integration test suite that verifies upload and download behavior against a LocalStack S3 instance started automatically via Testcontainers, requiring no manual environment setup beyond a running Docker daemon.

#### Scenario: Test suite starts LocalStack automatically
- **WHEN** the integration test suite runs (e.g. via `mvn test`)
- **THEN** a LocalStack container is started via Testcontainers as part of the test lifecycle, without requiring a pre-existing external LocalStack instance

#### Scenario: Test bucket is created by the test itself
- **WHEN** the integration test suite initializes
- **THEN** the test bucket used for assertions is created against the running LocalStack container before any put/get scenario executes
