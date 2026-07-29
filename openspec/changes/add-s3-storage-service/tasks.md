## 1. Project scaffold

- [x] 1.1 Generate Maven project structure (`pom.xml`, `src/main/java`, `src/main/resources`, `src/test/java`) targeting Java 21
- [x] 1.2 Add Spring Boot parent/BOM and `spring-boot-starter` dependency
- [x] 1.3 Add AWS SDK v2 BOM and `software.amazon.awssdk:s3` dependency
- [x] 1.4 Add test dependencies: `spring-boot-starter-test`, JUnit 5, Testcontainers core, Testcontainers LocalStack module
- [x] 1.5 Configure Maven Surefire/Failsafe (or equivalent) so integration tests run via `mvn test`

## 2. Configuration

- [x] 2.1 Create `S3Properties` (`@ConfigurationProperties`) for bucket name, region, and optional endpoint override
- [x] 2.2 Create `S3ClientConfig` with an `S3Client` `@Bean`: default profile uses default credential provider chain and no endpoint override
- [x] 2.3 Add `application.yml` (default/real-AWS profile) with bucket/region properties
- [x] 2.4 Add `application-local.yml` with LocalStack endpoint override and static dummy credentials (`test`/`test`)

## 3. Service implementation

- [x] 3.1 Implement `S3StorageService.putObject(bucket, key, bytes)` uploading via `S3Client`
- [x] 3.2 Implement `S3StorageService.getObject(bucket, key)` downloading and returning bytes via `S3Client`

## 4. Integration tests

- [ ] 4.1 Add `S3StorageServiceIT` annotated with `@Testcontainers`, declaring a `LocalStackContainer` with the S3 service enabled
- [ ] 4.2 Wire the test Spring context (or manually built `S3Client`) to the container's S3 endpoint and dummy credentials
- [ ] 4.3 Create the test bucket in `@BeforeAll`/`@BeforeEach` against the running container
- [ ] 4.4 Write test: successful upload results in an object existing in the bucket
- [ ] 4.5 Write test: successful download returns previously stored content
- [ ] 4.6 Write test: round-trip put-then-get returns byte-identical content

## 5. Verification

- [ ] 5.1 Run `mvn test` locally with Docker running and confirm all tests pass with no manual setup steps
- [ ] 5.2 Confirm the default (non-local) profile compiles/wires correctly without requiring LocalStack (no runtime verification against real AWS needed)
