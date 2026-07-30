## ADDED Requirements

### Requirement: Send a message to the outbound queue
The system SHALL provide a service that sends a byte payload to a configured outbound SQS queue (`out-queue`), using AWS SDK v2, and returns the resulting message ID.

#### Scenario: Successful send
- **WHEN** `OutboundQueueSender.sendMessage` is called with byte content
- **THEN** a message containing that content is sent to `out-queue` and a message ID is returned

### Requirement: Receive a message from the inbound queue without deleting it
The system SHALL provide a service that receives a message from a configured inbound SQS queue (`in-queue`) and returns its body, receipt handle, and message ID, without deleting the message from the queue.

#### Scenario: Successful receive
- **WHEN** `InboundQueueReceiver.receiveMessage` is called and a message is available on `in-queue`
- **THEN** the method returns the message's byte content, its SQS receipt handle, and its message ID

#### Scenario: Message remains after receive
- **WHEN** a message is received via `receiveMessage`
- **THEN** the message is not deleted from `in-queue` as a side effect of receiving it

#### Scenario: No message available
- **WHEN** `InboundQueueReceiver.receiveMessage` is called and no message is available on `in-queue`
- **THEN** the method indicates no message was received (e.g. an empty result), without throwing an error

### Requirement: Explicitly delete a received message
The system SHALL provide a way to delete a previously received message from the inbound queue using its receipt handle, as a separate step from receiving it.

#### Scenario: Successful delete after processing
- **WHEN** `InboundQueueReceiver.deleteMessage` is called with the receipt handle of a previously received message
- **THEN** the message is removed from `in-queue` and is not returned by a subsequent `receiveMessage` call

### Requirement: Round-trip send, receive, and delete
The system SHALL support sending a message to `out-queue`, and separately, receiving and deleting a message from `in-queue`, as independent operations against distinct queues.

#### Scenario: Send then independently receive and delete
- **WHEN** a message is sent to `out-queue` via `OutboundQueueSender`, and a message is received from and then deleted on `in-queue` via `InboundQueueReceiver`
- **THEN** both operations succeed independently, since `out-queue` and `in-queue` are distinct queues with no direct relationship in this system

### Requirement: Environment-specific SQS endpoint configuration
The system SHALL determine the SQS endpoint and credentials from Spring configuration profiles, without any code branching in the service layer, consistent with how the S3 client is configured.

#### Scenario: Default profile targets real AWS
- **WHEN** the application runs with no profile or a non-local profile active
- **THEN** the `SqsClient` bean uses the AWS default credential provider chain and no endpoint override, targeting real AWS SQS

#### Scenario: Local/test profile targets LocalStack
- **WHEN** the application or test runs with the `local` (or test) profile active
- **THEN** the `SqsClient` bean uses a static dummy credentials provider and an endpoint override pointing at the configured LocalStack endpoint

### Requirement: Hermetic integration test suite via LocalStack
The system SHALL include an integration test suite that verifies send, receive, and delete behavior against a LocalStack SQS instance, requiring no manual environment setup beyond a running Docker daemon.

#### Scenario: Test suite starts LocalStack automatically
- **WHEN** the integration test suite runs (e.g. via `mvn test`)
- **THEN** a LocalStack container providing SQS is available via Testcontainers as part of the test lifecycle, without requiring a pre-existing external LocalStack instance

#### Scenario: Test queues are created by the test itself
- **WHEN** the integration test suite initializes
- **THEN** both `out-queue` and `in-queue` are created against the running LocalStack container before any send/receive/delete scenario executes
