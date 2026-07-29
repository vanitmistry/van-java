## Context

Greenfield repository — no existing Java code, build tooling, or specs. This change establishes the first capability: a small Spring Boot service that reads and writes objects to an S3 bucket, with a fully local, hermetic test suite (LocalStack via Testcontainers).

## Goals / Non-Goals

**Goals:**
- Provide a working `S3StorageService` with `putObject`/`getObject` methods, usable directly (no HTTP layer).
- Same service code path works against real AWS and against LocalStack — only configuration differs, never code branching.
- `mvn test` is self-contained: no manual `docker-compose up`, no pre-existing bucket, no AWS credentials required to run the test suite.

**Non-Goals:**
- No REST/web controller or other external API surface.
- No production credential management beyond relying on the AWS default credential provider chain.
- No object listing, deletion, versioning, or multipart upload — only single-object put/get.
- No CI pipeline configuration (Docker-in-CI setup is left to whoever wires up the pipeline).

## Decisions

**AWS SDK v2, not v1.**
v1 (`com.amazonaws`) is in maintenance mode; v2 (`software.amazon.awssdk`) is the current SDK and has first-class `endpointOverride()` support on client builders, which is exactly the mechanism needed to point the same client code at LocalStack in tests. No alternative considered.

**Spring profile-based configuration, not code branching.**
An `S3Properties` (`@ConfigurationProperties`) bean plus profile-specific YAML (`application.yml` for default/real-AWS, `application-local.yml` for LocalStack) controls the `S3Client` bean's endpoint override and credentials provider. The service class itself has no knowledge of "am I running against LocalStack." Alternative considered: inject the endpoint via a system property/env var read directly in code — rejected because it scatters environment-awareness into business logic instead of keeping it in config/bean wiring, which is the idiomatic Spring approach.

**Testcontainers' LocalStack module over docker-compose.**
Testcontainers starts and stops the LocalStack container as part of the test JVM lifecycle, so `mvn test` needs only a running Docker daemon — no separate compose file to remember to start, no port clashes left behind after a forgotten teardown. Alternative considered: `docker-compose.yml` + tests assuming `localhost:4566` is already up — rejected because it's easy to forget to start it, and CI would need extra setup steps to bring it up before the test phase.

**Bucket created in test setup (`@BeforeAll`/`@BeforeEach`), not a LocalStack init script.**
Keeps the whole integration test self-contained in one file — anyone reading the test sees exactly what preconditions exist. Alternative considered: LocalStack `docker-entrypoint-initaws.d` init scripts — rejected as unnecessary indirection for a single bucket in a demo project.

**Default credential provider chain for the real-AWS profile.**
No credentials are hardcoded or stored in the repo. Real AWS usage relies on whatever the default provider chain resolves (env vars, shared config/profile, instance/task role). This is standard SDK v2 behavior and requires no extra code.

## Risks / Trade-offs

- [Docker not available in some environments (e.g. certain CI runners, restricted dev machines)] → Integration tests will fail to start LocalStack; document Docker as a prerequisite in the project README/tasks.
- [LocalStack S3 behavior can diverge from real AWS S3 in edge cases (e.g. some error responses, advanced features)] → Acceptable for this project's scope (basic put/get only); not a concern for the operations covered here.
- [Profile misconfiguration could accidentally point "local" credentials/endpoint at production] → Mitigated by keeping the LocalStack endpoint override and dummy credentials strictly under the `local`/`test` Spring profile, never the default.

## Open Questions

None outstanding — scope and approach were confirmed during exploration (see conversation history / `/opsx:explore` session that preceded this proposal).
