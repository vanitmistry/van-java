## Why

The project currently only talks to S3. We need the same kind of basic, hermetically-tested reference for AWS SQS: sending messages to an outbound queue and receiving/deleting messages from an inbound queue, so the project demonstrates a second core AWS integration pattern alongside object storage.

## What Changes

- Add AWS SDK v2 SQS support with two direction-specific service classes:
  - `OutboundQueueSender`: sends a message to the configured `out-queue`, resolving its queue URL eagerly at startup.
  - `InboundQueueReceiver`: receives a message from the configured `in-queue` (without deleting it) and exposes an explicit `deleteMessage` the caller invokes after successful processing.
- Message bodies are `byte[]` on the public API (base64-encoded/decoded internally, since SQS bodies are natively text), matching the `byte[]` shape already used by `S3StorageService`.
- New `SqsProperties`/`SqsClientConfig` following the same profile-driven pattern as the existing S3 config: default profile targets real AWS via the default credential provider chain; `local`/test profile targets LocalStack via endpoint override and static dummy credentials.
- New integration test (`SqsMessagingIT`) that creates both queues in `@BeforeAll` against LocalStack and exercises send → receive → delete.
- **Test infrastructure refactor (existing code):** introduce a shared LocalStack container (S3 + SQS services enabled) used by both the new SQS test and the existing `S3StorageServiceIT`, replacing each test's standalone container. This changes only test plumbing in the already-shipped `s3-storage` capability — its requirements/behavior are unchanged.

## Capabilities

### New Capabilities
- `sqs-messaging`: sending a message to an outbound SQS queue, and receiving and deleting messages from an inbound SQS queue, backed by AWS SDK v2, configurable to target either real AWS or a LocalStack instance for testing.

### Modified Capabilities
- None. The shared-LocalStack-container refactor touches `s3-storage`'s test implementation only, not its requirements, so no delta spec is needed against `s3-storage`.

## Impact

- New dependencies: none beyond what's already present (AWS SDK v2 BOM already covers the `sqs` module; Testcontainers LocalStack module already present).
- New code: `SqsProperties`, `SqsClientConfig`, `OutboundQueueSender`, `InboundQueueReceiver`, `SqsMessage` value type, `SqsMessagingIT`.
- Modified code: `S3StorageServiceIT` refactored to use a new shared `AbstractLocalStackIT` base class instead of its own `@Container` field; no change to `S3StorageService`, `S3ClientConfig`, or `S3Properties`.
- Non-goal: creating `out-queue`/`in-queue` in real AWS is out of scope, same as bucket provisioning was for S3 — assumed to exist via external infrastructure-as-code in production.
