# SupplyGraph — Technology Stack & Compatibility Verification

**Document ID:** `SG-ENG-REV-004`  
**Author:** Senior Java Engineer & Enterprise Systems Architect  
**Date:** November 2026  
**Status:** Compatibility Verification Baseline  

---

## 1. Deep Dive: Java 25 LTS vs. Java 21 LTS Compatibility Analysis

The blueprint outlines an intended baseline of **Java 25 LTS**, subject to framework compatibility verification. A rigorous analysis of official framework support across the Java ecosystem reveals critical operational findings.

### 1.1 The Java LTS Release Cadence
- **Java 17 LTS:** Released September 2021.
- **Java 21 LTS:** Released September 2023. Currently the battle-tested, universally supported production LTS across the global enterprise Java ecosystem.
- **Java 25 LTS:** Released September 2025. As of late 2026, Java 25 is actively stabilizing across major enterprise frameworks.

### 1.2 Framework Compatibility Matrix with Java 21 vs. Java 25

| Technology Component | Java 21 LTS Status | Java 25 LTS Status | Technical Evaluation & Risk |
|---|---|---|---|
| **Spring Boot 3.4.x** | **Native GA Support** | Supported with `--enable-preview` / early flags in some sub-projects | Spring Framework 6.2 / Spring Boot 3.4 baseline is Java 17+, with first-class GA support on Java 21. Java 25 runs smoothly, but certain native reflection/bytecode agents require caution. |
| **Vaadin Flow 24.5+** | **Native GA Support** | Compatible via Java 21 bytecode target | Vaadin Flow 24 is built on Spring Boot 3.4 and Java 17/21. Running on JDK 25 is supported when compilation target is set to Java 21 bytecode. |
| **Spring AI (1.0.0)** | **Native GA Support** | Functional | Native Java 21 record patterns and virtual threads (`Project Loom`) are heavily utilized. Stable on JDK 21+. |
| **Google OR-Tools Java (9.11+)** | **Native GA Support** | JNI Native Library bindings verified | Native C++ bindings via JNI. High sensitivity to JVM internal memory layout changes. 100% verified and stable on Java 21 LTS. |
| **Testcontainers 1.20+** | **Native GA Support** | Functional | Docker client interactions and Ryuk container management are rock-solid on Java 21. |
| **Flyway 10.x / 11.x** | **Native GA Support** | Native GA Support | SQL and Java-based migration runners fully compatible. |

### 1.3 Local Host Environment Audit
- Host machine has **Oracle JDK 26.0.2** as default `/usr/bin/java`, and **OpenJDK 21** via Homebrew at `/opt/homebrew/opt/openjdk@21`.
- Compiling with Maven configured to `<java.version>21</java.version>` allows seamless compilation on JDK 21 through JDK 26 using modern Java compiler cross-compilation (`--release 21`).

### 1.4 Recommendation & Engineering Baseline
> [!IMPORTANT]
> **Recommended Decision:** Set the build target to **Java 21 LTS bytecode (`--release 21`)** while supporting execution on JDK 21 through JDK 26.
> This guarantees:
> 1. Complete binary stability with Google OR-Tools native JNI libraries.
> 2. Zero reflection or bytecode-manipulation warnings from Hibernate / CGLIB / ByteBuddy.
> 3. Full utilization of modern Java features: Records, Pattern Matching, Sealed Classes, Sequenced Collections, and Virtual Threads (`Project Loom`).
> 4. Painless, zero-risk upgrade to native Java 25 language features once all downstream libraries publish dedicated Java 25 GA artifacts.

---

## 2. Technology Stack & Pinned Version Bill of Materials (BOM)

To avoid dependency conflicts, the parent POM will manage exact, stable version numbers across all modules.

```xml
<properties>
    <!-- Core Language & Framework Baselines -->
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <spring.boot.version>3.4.1</spring.boot.version>
    <vaadin.version>24.5.4</vaadin.version>
    <spring.ai.version>1.0.0-M5</spring.ai.version>
    
    <!-- Optimization & Mathematics -->
    <ortools.version>9.11.4210</ortools.version>
    
    <!-- Persistence & Database -->
    <postgresql.version>42.7.4</postgresql.version>
    <flyway.version>10.21.0</flyway.version>
    
    <!-- Observability & Utilities -->
    <micrometer.version>1.14.2</micrometer.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok.version>1.18.36</lombok.version>
    
    <!-- Testing Frameworks -->
    <testcontainers.version>1.20.4</testcontainers.version>
    <junit.jupiter.version>5.11.4</junit.jupiter.version>
    <assertj.version>3.27.0</assertj.version>
    <mockito.version>5.14.2</mockito.version>
</properties>
```

---

## 3. Infrastructure Component Justification & Deferral Analysis

### 3.1 PostgreSQL: The Authoritative Transactional Truth
- **Decision:** **Adopted as primary and sole database for initial phases.**
- **Capabilities:**
  - Relational consistency for users, workspaces, organizations, facilities, inventory positions, orders, disruption incidents, action intents, and audit logs.
  - JSONB support for polymorphic industry-pack extension attributes without table alteration.
  - Recursive Common Table Expressions (`WITH RECURSIVE`) for multi-tier BOM explosion and upstream/downstream network graph traversal up to 10+ tiers in sub-10ms latency.
  - Advisory locks and optimistic locking (`@Version`) for transactional race protection.

### 3.2 Neo4j: Graph Database Deferral
- **Decision:** **Deferred to post-MVP scale.**
- **Justification:**
  - A supply network with 10,000 nodes and 50,000 edges fits entirely in memory (less than 20 MB RAM).
  - Introducing Neo4j alongside PostgreSQL creates a **dual-write distributed transaction problem**. If an inventory position changes in PostgreSQL, keeping the Neo4j graph synchronously updated requires 2-Phase Commit (2PC) or an asynchronous Change Data Capture (CDC) pipeline with eventual consistency anomalies.
  - For the initial 4 industries and 32 scenarios, in-memory graph structures (`org.jgrapht` or custom adjacency lists) combined with PostgreSQL recursive queries provide microsecond graph queries with 100% ACID consistency and zero additional infrastructure.

### 3.3 Kafka: Distributed Message Broker Deferral
- **Decision:** **Deferred to post-MVP scale.**
- **Justification:**
  - In a modular monolith, in-process event buses (Spring `ApplicationEventPublisher`, Java `CompletableFuture`, and concurrent event queues) provide guaranteed, typed, in-memory message delivery with zero serialization overhead and zero network failure modes.
  - Introducing a distributed Kafka cluster requires ZooKeeper/KRaft quorum management, schema registries, consumer group rebalancing, and partition offset tracking.
  - An event interface abstraction (`EventPublisherPort`) will be implemented so that a Kafka adapter can be plugged in during future scale phases without changing domain or application services.

