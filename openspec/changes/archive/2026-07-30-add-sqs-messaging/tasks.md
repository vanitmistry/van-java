## 1. Configuration

- [x] 1.1 Create `SqsProperties` (`@ConfigurationProperties`) for out-queue name, in-queue name, region, and optional endpoint override
- [x] 1.2 Create `SqsClientConfig` with an `SqsClient` `@Bean`: default profile uses default credential provider chain and no endpoint override, mirroring `S3ClientConfig`
- [x] 1.3 Add out-queue-name/in-queue-name properties to `application.yml` (default/real-AWS profile)
- [x] 1.4 Add out-queue-name/in-queue-name properties to `application-local.yml` (LocalStack endpoint override already present for S3; reuse the same endpoint)

## 2. Service implementation

- [x] 2.1 Create `SqsMessage` value type: `body` (byte[]), `receiptHandle` (String), `messageId` (String)
- [x] 2.2 Implement `OutboundQueueSender`: resolve `out-queue` URL eagerly in the constructor via `getQueueUrl`; `sendMessage(byte[])` base64-encodes and sends, returning the message ID
- [x] 2.3 Implement `InboundQueueReceiver`: resolve `in-queue` URL eagerly in the constructor via `getQueueUrl`; `receiveMessage()` returns `Optional<SqsMessage>` (base64-decoded body), does not delete; `deleteMessage(String receiptHandle)` deletes explicitly

## 3. Shared LocalStack test infrastructure (refactor of existing code)

- [x] 3.1 Create `AbstractLocalStackIT` base test class: single shared `@Container LocalStackContainer` (pinned to `localstack/localstack:4.4.0`) with `Service.S3` and `Service.SQS` both enabled
- [x] 3.2 Refactor existing `S3StorageServiceIT` to extend `AbstractLocalStackIT` instead of declaring its own standalone `@Container` field; keep its bucket creation and S3-specific `@DynamicPropertySource` entries in the subclass
- [x] 3.3 Re-run `S3StorageServiceIT` after the refactor and confirm its existing 3 scenarios still pass unchanged

## 4. Integration tests

- [x] 4.1 Add `SqsMessagingIT` extending `AbstractLocalStackIT`, with SQS-specific `@DynamicPropertySource` entries (endpoint override, region)
- [x] 4.2 Create `out-queue` and `in-queue` in `@BeforeAll` against the shared LocalStack container
- [x] 4.3 Write test: successful send to `out-queue` returns a message ID
- [x] 4.4 Write test: receive from `in-queue` returns body/receiptHandle/messageId and the message is NOT deleted as a side effect
- [x] 4.5 Write test: `receiveMessage` on an empty `in-queue` returns an empty result rather than throwing
- [x] 4.6 Write test: `deleteMessage` removes the message so a subsequent `receiveMessage` no longer returns it
- [x] 4.7 Write test: send to `out-queue` and receive+delete on `in-queue` succeed independently in the same test run

## 5. Verification

- [x] 5.1 Run `mvn test` locally with Docker running and confirm all tests pass (S3 + SQS) with no manual setup steps and only one LocalStack container started
- [x] 5.2 Confirm the default (non-local) profile compiles/wires correctly without requiring LocalStack (no runtime verification against real AWS needed)
