# SupplyGraph — Comprehensive Testing Guide

**Document ID:** `SG-DOC-005`  
**Phase:** Phase 1 (Foundation Baseline)  
**Status:** Official Testing Standards  

---

## 1. Testing Philosophy & Pyramid

SupplyGraph enforces a disciplined testing pyramid:

```
                  /\
                 /  \     End-to-End & Acceptance Tests (32 Scenarios)
                /    \    -------------------------------------------
               /  IT  \   Integration Tests (Testcontainers PostgreSQL 16)
              /--------\  -------------------------------------------
             /   ARCH   \ Architecture Boundary Tests (ArchUnit)
            /------------\-------------------------------------------
           /     UNIT     \ Unit & Domain Invariant Tests (JUnit 5 / AssertJ)
          /----------------\
```

---

## 2. Test Suites by Module

| Module | Test Suite Name | Purpose | Target Execution Time |
|---|---|---|---|
| `supplygraph-core-domain` | `WorkspaceTest` | Validates domain invariants, null-checks, and value object integrity. | < 50ms |
| `supplygraph-industry-api` | `IndustryPackExtensionTest` | Validates SPI contract and identifier formatting. | < 20ms |
| `supplygraph-concurguard` | `ConcurGuardEngineTest` | Validates enum coverage for all 7 conflict types and arbitration outcomes. | < 20ms |
| `supplygraph-simulation-engine` | `SimulationClockTest` | Verifies monotonic forward time progression and backward progression rejection. | < 20ms |
| `supplygraph-core-application` | `EventPublisherPortTest` | Validates application event publishing port contracts. | < 20ms |
| `supplygraph-industry-ev` | `EvBatteryIndustryPackTest` | Validates EV battery pack initialization and 8 scenario definitions. | < 20ms |
| `supplygraph-industry-pharma` | `PharmaIndustryPackTest` | Validates Pharma pack initialization and 8 scenario definitions. | < 20ms |
| `supplygraph-industry-semiconductor` | `SemiconductorIndustryPackTest` | Validates Semiconductor pack initialization and 8 scenario definitions. | < 20ms |
| `supplygraph-industry-automotive` | `AutomotiveIndustryPackTest` | Validates Automotive pack initialization and 8 scenario definitions. | < 20ms |
| `supplygraph-adapter-ai` | `DeterministicFakeAiClientTest` | Validates offline fake AI proposal generation. | < 20ms |
| `supplygraph-adapter-persistence` | `FlywayMigrationIntegrationTest` | Spins up real PostgreSQL 16 container, executes Flyway migrations, asserts tables. | ~3-5s |
| `supplygraph-web` | `SupplyGraphApplicationTests` | Verifies complete Spring Boot context bootstrap. | ~2s |
| `supplygraph-web` | `ArchitectureBoundaryTests` | ArchUnit tests asserting pure domain, acyclic packages, and layer boundaries. | ~1s |
| `supplygraph-web` | `VaadinViewsTest` | Validates Vaadin Flow view annotations, layouts, and component hierarchies. | < 200ms |

---

## 3. Architecture Boundary Verification (ArchUnit)

ArchUnit tests in `supplygraph-web` automatically fail the build if:
1. `supplygraph-core-domain` imports any class outside `java.*` or `com.supplygraph.domain.*`.
2. Any package slice contains circular dependencies.
3. Classes outside `supplygraph-web` import Vaadin Flow.
4. Classes outside `supplygraph-adapter-persistence` import JPA or Hibernate.

To execute ArchUnit tests:
```bash
./mvnw test -pl supplygraph-web -Dtest=ArchitectureBoundaryTests
```

