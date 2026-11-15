# SupplyGraph — Local Development Setup & Toolchain Guide

**Document ID:** `SG-DOC-001`  
**Phase:** Phase 1 (Foundation Baseline)  
**Status:** Official Developer Setup Guide  

---

## 1. Prerequisites

To build and run SupplyGraph locally, ensure the following tools are installed:

| Tool | Minimum Version | Recommended / Target | Notes |
|---|---|---|---|
| **Java Development Kit (JDK)** | JDK 21 LTS | OpenJDK 21 LTS (`brew install openjdk@21`) | Compiler target is pinned to Java 21 (`--release 21`). Host JDKs 21 through 26 are supported. |
| **Maven** | Not required | **Bundled Maven Wrapper (`./mvnw`)** | Apache Maven 3.9.9 is embedded via the wrapper. |
| **Docker Engine** | Docker 24+ | Docker Desktop / OrbStack / Podman | Required for Testcontainers integration testing and local PostgreSQL 16 Compose. |
| **Git** | 2.40+ | Latest | SSH access to repository configured. |

---

## 2. Environment Configuration

### 2.1 Set `JAVA_HOME`
On macOS (with Homebrew OpenJDK 21):
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
```

Verify your Java version:
```bash
java -version
# Expected: openjdk version "21.0.x"
```

### 2.2 Verify Maven Wrapper
```bash
./mvnw -version
# Expected: Apache Maven 3.9.9
```

---

## 3. Building the Modular Monolith

### 3.1 Clean Compilation across All 12 Modules
```bash
./mvnw clean compile
```

### 3.2 Running the Full Automated Test Suite
```bash
./mvnw test
```

### 3.3 Running Architecture Boundary Verification Tests (ArchUnit)
```bash
./mvnw test -pl supplygraph-web -Dtest=ArchitectureBoundaryTests
```

### 3.4 Running Flyway Database Migration Tests (Testcontainers)
```bash
./mvnw test -pl supplygraph-adapter-persistence -Dtest=FlywayMigrationIntegrationTest
```

---

## 4. Running the Application Locally

### 4.1 Option A: Local In-Memory Development (Quickest)
By default, the application boots using the default profile or test profile with in-memory persistence if PostgreSQL is not active:
```bash
./mvnw spring-boot:run -pl supplygraph-web
```

### 4.2 Option B: Local PostgreSQL via Docker Compose
To run against a local PostgreSQL 16 container:
```bash
# Start PostgreSQL
docker compose up -d

# Verify database is healthy
docker compose ps

# Run SupplyGraph web application
./mvnw spring-boot:run -pl supplygraph-web
```

---

## 5. Verifying Operational Endpoints

Once booted, verify the following endpoints:

1. **Vaadin Flow Web Application:**  
   Open [http://localhost:8080/](http://localhost:8080/) in your web browser. Displays the SupplyGraph overview dashboard.
2. **System Health View:**  
   Open [http://localhost:8080/health](http://localhost:8080/health) in your web browser. Displays runtime diagnostics.
3. **Spring Boot Actuator Health Probe:**  
   `curl -s http://localhost:8080/actuator/health`  
   Expected JSON response: `{"status":"UP"}`.

