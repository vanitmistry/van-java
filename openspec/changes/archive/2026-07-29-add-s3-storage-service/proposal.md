## Why

The project currently has no code — just a placeholder repo. We need a minimal, working reference for reading and writing objects to an AWS S3 bucket from a Java 21 / Spring Boot service, with a test suite that runs entirely locally (no real AWS account needed) via LocalStack.

## What Changes

- New Maven-based Java 21 project scaffold (Spring Boot, no web layer).
- Add AWS SDK v2 (`software.amazon.awssdk:s3`) dependency and an `S3Client` bean, configured via Spring profiles:
  - Default profile: real AWS via the default credential provider chain (no hardcoded credentials).
  - `local`/`test` profile: S3 endpoint override pointed at LocalStack, with static dummy credentials (`test`/`test`).
- New `S3StorageService` with `putObject` (upload) and `getObject` (download) methods.
- New integration test suite using Testcontainers' LocalStack module: spins up LocalStack automatically, creates the test bucket in `@BeforeAll`/`@BeforeEach`, and exercises put/get end-to-end. `mvn test` requires only Docker, no manual setup.

## Capabilities

### New Capabilities
- `s3-storage`: Read (download) and write (upload) operations against an S3 bucket, backed by AWS SDK v2, configurable to target either real AWS or a LocalStack instance for testing.

### Modified Capabilities
- None (greenfield project, no existing specs).

## Impact

- New Maven project structure (`pom.xml`, `src/main/java`, `src/test/java`, `src/main/resources`).
- New dependencies: Spring Boot starter, AWS SDK v2 S3 module, Testcontainers (core + LocalStack module), JUnit 5.
- Requires Docker to be available locally/in CI to run the integration test suite.
- No production infrastructure changes — this establishes the service and its tests only; bucket provisioning and real AWS credentials/config are out of scope.
