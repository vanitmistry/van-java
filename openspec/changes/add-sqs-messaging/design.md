## Context

The project has one existing AWS integration (`s3-storage`): `S3Properties`/`S3ClientConfig` for profile-driven client configuration, `S3StorageService` for put/get, and `S3StorageServiceIT` running against a standalone Testcontainers LocalStack container pinned to `localstack/localstack:4.4.0` (chosen because newer LocalStack tags require a paid license token). This change adds a second AWS integration (SQS) and, because both now need LocalStack, introduces shared test infrastructure that also touches the existing S3 test.

## Goals / Non-Goals

**Goals:**
- Send a `byte[]` payload to `out-queue` and receive/delete messages from `in-queue`, using AWS SDK v2, with the same profile-driven real-AWS-vs-LocalStack configuration pattern already used for S3.
- Give the caller explicit control over message deletion (receive does not implicitly delete) — closer to real SQS consumer semantics than the S3 capability's simpler one-shot get.
- Consolidate LocalStack container startup for S3 and SQS tests into one shared container, since both capabilities now need LocalStack and starting two containers per test run is wasted time with no benefit.

**Non-Goals:**
- No queue provisioning in real AWS — `out-queue`/`in-queue` are assumed to already exist via external IaC, same non-goal as S3 bucket provisioning.
- No message attributes, batching, FIFO queue semantics, or dead-letter queue handling — single-message send/receive/delete only.
- No REST/web layer — service classes are exercised directly, same as `S3StorageService`.
- No listener-based/continuous consumption (e.g. `@SqsListener`-style polling loop) — `receiveMessage()` is a single synchronous call, mirroring the request/response shape of `S3StorageService`.

## Decisions

**Direction-specific classes (`OutboundQueueSender`, `InboundQueueReceiver`), not one generic parameterized service.**
Unlike S3 where the bucket is a parameter on every call, SQS's `out-queue`/`in-queue` are fixed, distinct queues with no operation that ever targets "the other one." Binding each class to its own queue name/URL removes a parameter that would otherwise always be the same value at every call site. Alternative considered: one `SqsMessagingService` taking `queueName` per call, mirroring `S3StorageService`'s `bucket`/`key` parameters — rejected because there is no real use case in this project for sending to `in-queue` or receiving from `out-queue`; the parameter would exist only in theory.

**`byte[]` message bodies, base64-encoded/decoded internally.**
SQS message bodies are native UTF-8 strings, so a `String`-typed API would be the more natural fit for SQS specifically. `byte[]` was chosen anyway so `OutboundQueueSender`/`InboundQueueReceiver` present the same shape as `S3StorageService.putObject`/`getObject`, keeping the two AWS integrations in this project consistent for callers who don't care about the underlying transport. The base64 transcoding is entirely internal — callers never see or handle it.

**Caller-managed delete: `receiveMessage()` returns a receipt handle; a separate `deleteMessage(receiptHandle)` must be called explicitly.**
This matches real SQS consumer semantics: a received message isn't gone, it's just temporarily invisible, and only an explicit delete removes it permanently. Auto-deleting inside `receiveMessage()` would hide that distinction and make failure handling (e.g. crash before processing completes) silently lossy. Alternative considered: receive-and-auto-delete for symmetry with S3's one-shot `getObject` — rejected as it would misrepresent SQS's at-least-once delivery model for a demo meant to be a realistic reference.

**Eager, fail-fast queue URL resolution in the constructor.**
`OutboundQueueSender`/`InboundQueueReceiver` call `SqsClient.getQueueUrl(...)` once when the bean is constructed and cache the result, rather than resolving lazily on first use. This surfaces a missing/misconfigured queue immediately at application startup instead of on the first real send/receive call, matching the project's existing preference for explicit, upfront configuration (profile-driven endpoint override) over deferred failure. Alternative considered: lazy resolution cached on first use — rejected because it would let an app with a broken queue configuration start successfully and only fail later, under real traffic.

**Shared LocalStack container across S3 and SQS integration tests.**
A new `AbstractLocalStackIT` base class declares a single `@Container LocalStackContainer` with `Service.S3` and `Service.SQS` both enabled, still pinned to `localstack/localstack:4.4.0`. Both `S3StorageServiceIT` (refactored) and the new `SqsMessagingIT` extend it. Alternative considered: give the new SQS test its own independent LocalStack container (as originally done for S3) — rejected because running two LocalStack containers per `mvn test` invocation roughly doubles container-startup overhead for no isolation benefit; the two test classes don't interact and sharing a container doesn't introduce test coupling beyond both depending on the same base class.

## Risks / Trade-offs

- [Eager queue-URL resolution means the whole Spring context fails to start if `out-queue` or `in-queue` doesn't exist yet, even for a deployment that won't touch SQS immediately] → Accepted: this project already assumes queues/buckets are provisioned before the app starts (see Non-Goals), so failing fast is consistent with that assumption rather than a new risk.
- [Refactoring `S3StorageServiceIT` to use a shared base class touches already-shipped, archived code] → No behavior/requirement change to `s3-storage`; risk is purely "did the refactor preserve the existing test's behavior," mitigated by re-running `S3StorageServiceIT` after the refactor and confirming it still passes unchanged.
- [`byte[]`-over-SQS's-native-string API adds a base64 encode/decode step that has no functional benefit for SQS itself] → Accepted trade-off for cross-capability API consistency in this reference project; revisit if a real consumer needs plain-text bodies.
- [A shared LocalStack container means an SQS-side container misconfiguration could, in principle, break the S3 test too] → Mitigated by keeping service-specific setup (bucket creation, queue creation) in each test class's own `@BeforeAll`, not in the shared base class — the base class only owns container lifecycle, not test data.
