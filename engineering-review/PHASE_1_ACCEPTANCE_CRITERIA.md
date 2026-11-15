# SupplyGraph — Phase 1 Scope & Strict Acceptance Criteria

**Document ID:** `SG-ENG-REV-015`  
**Author:** Principal Software Architect & Quality Assurance Lead  
**Date:** November 2026  
**Status:** Pre-Implementation Architectural Validation  

---

## 1. Phase 1 Engineering Objective

The sole objective of **Phase 1: Build Foundation & Modular Monolith Skeleton** is to establish a rock-solid, hermetic engineering foundation for SupplyGraph.

> [!CRITICAL]
> **STRICT SCOPE BOUNDARY:**  
> **Phase 1 contains ZERO business domain functionality.**  
> - Do NOT implement real inventory algorithms.  
> - Do NOT implement AI agent prompt templates or LLM integrations.  
> - Do NOT implement industry pack scenarios.  
> - Do NOT implement Google OR-Tools optimization solvers.  
> - Do NOT implement ConcurGuard conflict resolution algorithms.  
> Phase 1 is exclusively focused on **toolchain bootstrap, module scaffolding, compiler settings, architectural test gates, database migration baseline, and application startup verification**.

---

## 2. In-Scope Deliverables for Phase 1

### 2.1 Toolchain & Build Automation
1. **Maven Wrapper:** Commit `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, and `mvnw.cmd` using Apache Maven 3.9.9.
2. **Root Parent POM (`pom.xml`):**
   - Inherits from `org.springframework.boot:spring-boot-starter-parent:3.4.1`.
   - Sets Java release target to 21 (`<java.version>21</java.version>`).
   - Declares comprehensive `<dependencyManagement>` for Vaadin 24.5.4, Spring AI 1.0.0-M5, Google OR-Tools 9.11.4210, Flyway 10.21.0, JGraphT 1.5.2, ArchUnit 1.3.0, and Testcontainers 1.20.4.
   - Enforces Checkstyle and compiler warning flags (`-Xlint:all,-processing`).

### 2.2 Approved Maven Module Skeletons (12 Modules)
Every module receives its dedicated `pom.xml`, `src/main/java/`, and `src/test/java/` directory structure:
1. `supplygraph-core-domain`
2. `supplygraph-industry-api`
3. `supplygraph-concurguard`
4. `supplygraph-simulation-engine`
5. `supplygraph-core-application`
6. `supplygraph-industry-ev`
7. `supplygraph-industry-pharma`
8. `supplygraph-industry-semiconductor`
9. `supplygraph-industry-automotive`
10. `supplygraph-adapter-persistence`
11. `supplygraph-adapter-ai`
12. `supplygraph-web`

### 2.3 Application Bootstrap & Framework Verification
1. **Spring Boot Entrypoint:** `com.supplygraph.web.SupplyGraphApplication` located in `supplygraph-web`.
2. **Actuator & Health Endpoints:** Exposes `/actuator/health` and `/actuator/info`.
3. **Vaadin Flow Shell:**
   - Minimal `MainLayout` with navigation bar.
   - `HomeView` mapped to `/` with system title and architectural status badge.
   - `HealthView` displaying application runtime and database connectivity status.
4. **PostgreSQL & Flyway Baseline:**
   - Baseline migration `V1__init_schema.sql` in `supplygraph-adapter-persistence/src/main/resources/db/migration/` establishing initial schema tables (`workspaces`, `audit_events`).
   - `application.yml` and `application-test.yml` with clear environment variable overrides.

### 2.4 Architecture & Quality Gate Tests
1. **ArchUnit Verification:** Automated architecture tests in `supplygraph-web/src/test/java` verifying:
   - `supplygraph-core-domain` depends only on Java standard library packages.
   - No circular dependencies exist across any package slices.
2. **Spring Context Smoke Test:** Verifies Spring Boot application context loads cleanly.
3. **Testcontainers Flyway Test:** Boots a disposable PostgreSQL 16 container, applies Flyway migrations, and verifies table creation.

---

## 3. Strict Acceptance Criteria Checklist

| Checkpoint | Command / Action | Expected Result | Pass / Fail Criteria |
|---|---|---|---|
| **AC-01: Hermetic Build** | `./mvnw -version` | Outputs Apache Maven 3.9.9 and JDK 21+ without error. | Pass: Zero requirement for pre-installed global Maven. |
| **AC-02: Clean Compilation** | `./mvnw clean compile` | Compiles all 12 modules with 0 errors and 0 circular dependency warnings. | Pass: `BUILD SUCCESS` across all 12 modules. |
| **AC-03: Architecture Boundaries** | `./mvnw test -pl supplygraph-web -Dtest=ArchitectureBoundaryTests` | ArchUnit asserts that `core-domain` has zero Spring/JPA/Vaadin imports. | Pass: 100% of ArchUnit rules pass without violation. |
| **AC-04: Flyway Migration Test** | `./mvnw test -pl supplygraph-adapter-persistence -Dtest=FlywayMigrationIntegrationTest` | Testcontainers spins up PostgreSQL 16; Flyway applies `V1__init_schema.sql` successfully. | Pass: Schema migration completes; table metadata verified. |
| **AC-05: Spring Context Bootstrap** | `./mvnw test -pl supplygraph-web -Dtest=SupplyGraphApplicationTests` | Spring Boot context loads with all module configurations active. | Pass: Application context boots in < 5 seconds. |
| **AC-06: Full Test Suite** | `./mvnw test` | Runs all foundation and architecture unit/integration tests across all 12 modules. | Pass: 100% tests pass; 0 failures; 0 errors; 0 skipped. |
| **AC-07: Local Application Boot** | `./mvnw spring-boot:run -pl supplygraph-web` | Application starts on port 8080; logs display ASCII banner and startup time. | Pass: Accessing `http://localhost:8080/` loads Vaadin navigation shell; `/actuator/health` returns `{"status":"UP"}`. |

---

## 4. Phase 1 Exit Gate Review

Phase 1 is officially complete **only when**:
1. All seven acceptance criteria (AC-01 through AC-07) have been executed and their exact command outputs verified.
2. A formal Phase 1 Completion Report is published listing every changed file, actual test counts, and verified execution logs.
3. The project stops and requests explicit user sign-off before initiating Phase 2.

