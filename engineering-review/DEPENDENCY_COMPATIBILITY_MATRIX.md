# SupplyGraph — Pinned Dependency Compatibility Matrix

**Document ID:** `SG-ENG-REV-012`  
**Author:** Senior Java Engineer & Enterprise Systems Architect  
**Date:** November 2026  
**Status:** Pre-Implementation Architectural Validation  

---

## 1. Deep Dive: Java 21 LTS vs. Java 25 LTS Runtime & Compiler Analysis

| Dimension | Java 21 LTS (JSR 396) | Java 25 LTS | Engineering Assessment & Recommendation |
|---|---|---|---|
| **Official Release Date** | September 2023 | September 2025 | Java 21 has over three years of worldwide enterprise production hardening. Java 25 is actively stabilizing downstream tools. |
| **LTS Vendor Support Window** | Active LTS through at least September 2028 (commercial extended through 2031+) | Active LTS through September 2030 | Java 21 provides immediate, zero-risk long-term commercial support across Eclipse Temurin, Amazon Corretto, Red Hat, and Oracle. |
| **Spring Boot 3.4 Compatibility** | **Native First-Class GA** | Early Access / Run-as-is | Spring Framework 6.2 and Spring Boot 3.4 target Java 17 and Java 21 as primary certified baselines. |
| **Vaadin Flow 24 Compatibility** | **Native First-Class GA** | Functional via Java 21 bytecode | Vaadin Flow 24 is built on Java 17/21. Running on JDK 25 requires Java 21 bytecode target. |
| **Google OR-Tools JNI Bindings** | **100% Certified & Verified** | JNI layout verification needed | Google OR-Tools relies on native C++ libraries connected via Java Native Interface (JNI). Any JVM internal memory layout changes in experimental JDKs can trigger native segmentation faults (`SIGSEGV`). Java 21 is rock-solid. |
| **Bytecode Instrumentation (ByteBuddy / CGLIB)** | **100% Certified** | Subject to JVM internals warnings | Hibernate and Mockito rely heavily on dynamic bytecode generation. Java 21 has zero bytecode reflection warnings. |
| **Language Feature Availability** | Virtual Threads (`Project Loom`), Records, Pattern Matching, Sealed Classes, Sequenced Collections | Derived language enhancements | All major architectural concurrency and domain modeling patterns needed for SupplyGraph are fully present in Java 21. |
| **Target Recommendation** | **COMPILE TARGET: Java 21 (`--release 21`)** | **HOST EXECUTION: JDK 21–26** | **RECOMMENDED BASELINE:** Pin Maven compiler to Java 21 bytecode (`<java.version>21</java.version>`). This guarantees 100% binary stability with OR-Tools while allowing compilation on newer host JDKs (including the host machine's JDK 26.0.2). |

---

## 2. Pinned Dependency Compatibility Matrix

Every dependency declared in the SupplyGraph Parent POM has been verified against official release notes and compatibility specifications.

| Group & Artifact ID | Selected Version | Official Source | Java Compatibility | Framework Compatibility | Support Status | Technical Reason for Selection |
|---|---|---|---|---|---|---|
| **`org.springframework.boot:spring-boot-starter-parent`** | `3.4.1` | Maven Central / Spring IO | Java 17, 21, 25 | Spring Framework 6.2.1 | **GA (Active)** | Current stable generation of Spring Boot. Provides auto-configuration, virtual thread support, and unified dependency management for Hibernate, Jackson, and Micrometer. |
| **`com.vaadin:vaadin-bom`** | `24.5.4` | Maven Central / Vaadin.com | Java 17, 21 | Spring Boot 3.3.x, 3.4.x | **GA (LTS line)** | Official LTS release of Vaadin Flow. Enables 100% Java-authored web UI, server-side push over WebSockets, and seamless integration with Spring Security. |
| **`org.springframework.ai:spring-ai-bom`** | `1.0.0-M5` | Spring Milestone Repo / Maven Central | Java 17, 21 | Spring Boot 3.4.x | **Milestone (Active)** | Unified Spring abstraction for LLMs (ChatClient, Tool Calling, Structured Output). *Justification below.* |
| **`com.google.ortools:ortools-java`** | `9.11.4210` | Maven Central / Google OR-Tools | Java 17, 21 | Independent (JNI) | **GA (Active)** | High-performance mathematical programming suite. Provides CP-SAT constraint programming and GLOP linear solver for deterministic supply chain allocation. |
| **`org.postgresql:postgresql`** | `42.7.4` | Maven Central / PostgreSQL.org | Java 8 through 25 | JDBC 4.2 / Hibernate 6.6 | **GA (Active)** | Official JDBC driver for PostgreSQL. Fully certified for PostgreSQL 14, 15, 16, and 17. |
| **`org.flywaydb:flyway-core`** | `10.21.0` | Maven Central / Redgate | Java 17, 21, 25 | Spring Boot 3.4.x, PostgreSQL | **GA (Active)** | Versioned relational database migration engine. Guarantees immutable, reproducible schema evolution across local and CI environments. |
| **`org.flywaydb:flyway-database-postgresql`** | `10.21.0` | Maven Central / Redgate | Java 17, 21, 25 | Flyway Core 10.21.0 | **GA (Active)** | Dedicated PostgreSQL dialect module required by Flyway 10+ for PostgreSQL 16+. |
| **`org.hibernate.orm:hibernate-core`** | `6.6.3.Final` (Managed by Boot) | Maven Central / Hibernate.org | Java 17, 21 | JPA 3.2 / Spring Data JPA | **GA (Active)** | Authoritative JPA provider managed by Spring Boot 3.4.1. Provides robust entity persistence and PostgreSQL dialect optimization. |
| **`org.jgrapht:jgrapht-core`** | `1.5.2` | Maven Central / JGraphT | Java 11, 17, 21 | Independent | **GA (Active)** | Ultra-fast in-memory graph data structure library. Provides directed acyclic graph (DAG) cycle detection, topological sorting, and shortest-path algorithms. |
| **`com.tngtech.archunit:archunit-junit5`** | `1.3.0` | Maven Central / ArchUnit.org | Java 17, 21, 25 | JUnit 5 | **GA (Active)** | Enforces architecture rules at build time. Used to fail the build if `core-domain` accesses Spring, JPA, Vaadin, or AI classes. |
| **`org.testcontainers:postgresql`** | `1.20.4` | Maven Central / Testcontainers | Java 17, 21, 25 | Docker 24+, JUnit 5 | **GA (Active)** | Spins up real, disposable PostgreSQL 16 containers for zero-mock integration testing of Flyway migrations and JPA queries. |
| **`org.junit.jupiter:junit-jupiter`** | `5.11.4` (Managed by Boot) | Maven Central / JUnit.org | Java 17, 21, 25 | Spring Test | **GA (Active)** | Standard unit and integration testing engine for modern Java. |
| **`org.assertj:assertj-core`** | `3.27.0` (Managed by Boot) | Maven Central / AssertJ | Java 17, 21, 25 | Independent | **GA (Active)** | Fluent, type-safe assertions for domain invariants and ConcurGuard conflict checks. |
| **`org.apache.maven.wrapper:maven-wrapper`** | `3.3.2` (Maven 3.9.9) | Apache Maven Official | Java 17, 21, 25 | POSIX / Windows | **GA (Active)** | Hermetic Maven build execution wrapper. Eliminates requirement for host-level Maven installation. |

---

## 3. Explicit Justification for Non-GA Dependencies (Spring AI)

### 3.1 Status Analysis
Spring AI is currently progressing through Milestone and Release Candidate cycles (`1.0.0-M5` / `1.0.0-RC1` moving to `1.0.0 GA`).
- **Why It Is Used:** Spring AI is the official Spring ecosystem abstraction for AI integration, replacing fragmented proprietary SDKs with a clean `ChatClient` interface, typed function calling (tools), and structured output binding.
- **Risk Mitigation Strategy:**
  1. *Complete Adapter Isolation:* Spring AI dependencies are restricted entirely to `supplygraph-adapter-ai`. No domain or application code imports any `org.springframework.ai.*` package.
  2. *Domain Ports & Inversion of Control:* Application orchestrators interact with an abstract interface (`AgentInferencePort`).
  3. *Zero Impact on Phase 1:* Phase 1 establishes build foundations, database migrations, and UI navigation. Spring AI is declared only in parent dependency management; no model API calls occur until Phase 5.
  4. *Deterministic Fake Adapter:* All automated unit and integration tests use `DeterministicFakeAiClient`, ensuring test execution is 100% independent of external AI availability or milestone API shifts.

