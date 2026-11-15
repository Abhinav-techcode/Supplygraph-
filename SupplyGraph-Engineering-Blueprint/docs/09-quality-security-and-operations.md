# 09 — Quality, Security and Operations

## Clean code standards

- Follow standard Java naming and formatting conventions.
- Prefer small cohesive classes and methods.
- Use constructor injection; avoid field injection.
- Keep controllers/views thin.
- Keep business rules in domain/application services, not UI components.
- Use interfaces at meaningful boundaries, not for every class.
- Avoid giant service classes, god objects and generic utility dumping grounds.
- Avoid unnecessary inheritance; prefer composition.
- Use immutable value objects where appropriate.
- Validate at system boundaries.
- Use explicit exceptions and error contracts.
- Avoid swallowing exceptions or logging-and-continuing on invalid state.
- No secrets, API keys or credentials in source control.
- No unexplained magic numbers; use named configuration and domain policies.

## Testing pyramid

### Unit tests
- Domain invariants and calculations.
- Conflict classification and arbitration.
- Industry rule evaluation.
- Optimization constraint construction.
- Simulation event ordering and determinism.

### Application tests
- Use-case orchestration.
- Authorization and workspace isolation.
- Agent proposal validation.
- State-version revalidation.
- Human approval workflow.

### Integration tests
- PostgreSQL persistence and migrations.
- Neo4j adapter if enabled.
- Kafka adapter if enabled.
- Spring AI adapter with deterministic fake provider.
- Vaadin/application startup and key UI flows.

### Scenario acceptance tests
Each of the 32 initial scenarios must have:
- Given state and data provenance.
- Trigger event.
- Expected impacted entities.
- Expected constraints and blocked actions.
- Expected candidate recovery behavior.
- Expected approval boundary.
- Assertions for deterministic results.

## Security

- Authentication and server-side authorization.
- Role and workspace/tenant isolation.
- Validate all IDs against workspace ownership.
- Protect against insecure direct object references.
- Store secrets outside Git.
- Use least-privilege database and service credentials.
- Validate uploads and limit size/type.
- Protect against prompt injection and tool misuse.
- Rate-limit expensive AI and simulation endpoints.
- Audit approvals and administrative changes.
- Define retention and privacy policies before production data is used.

## Operations

- Health and readiness endpoints.
- Structured logs with correlation IDs.
- Metrics for application, agent, optimization and simulation runs.
- Graceful shutdown.
- Database migration procedure.
- Backup and restore instructions.
- Local Docker Compose only for dependencies actually selected.
- Document configuration variables in `.env.example` without real secrets.
- Provide reproducible local development instructions.

## Definition of done

A feature is done only when:
- Acceptance criteria are met.
- Relevant automated tests pass.
- Authorization and validation are implemented.
- Errors and edge cases are handled.
- Documentation is updated.
- No secrets or generated artifacts are committed.
- Build and test results are reported accurately.
