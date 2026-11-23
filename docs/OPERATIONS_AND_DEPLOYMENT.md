# SupplyGraph — Operations & Production Deployment Runbook

**Version:** 1.0.0  
**Target Environments:** Docker Compose, Kubernetes, On-Premises Linux  
**Supported Database:** PostgreSQL 16+ Alpine  

---

### 1. Prerequisites

- **Java Runtime:** Eclipse Temurin OpenJDK 21 LTS (`JAVA_HOME` pointing to JDK 21).
- **Container Engine:** Docker Engine 24+ or Colima / OrbStack with Docker Compose V2.
- **Relational Database:** PostgreSQL 16+ with `uuid-ossp` or standard extension support.
- **Port Allocations:**
  - `8080`: SupplyGraph Web & Application Server.
  - `5432` / `5433`: PostgreSQL Database.

---

### 2. Local Docker Compose Quickstart

1. **Copy Environment Template:**
   ```bash
   cp .env.example .env
   ```

2. **Start PostgreSQL & SupplyGraph App:**
   ```bash
   docker compose up -d --build
   ```

3. **Verify Service Health:**
   ```bash
   docker compose ps
   curl -f http://localhost:8080/actuator/health
   ```

4. **Access Platform UI:**
   Navigate browser to: `http://localhost:8080/`


---

### 2b. Deploying (read this first)

**What you must set**

| Variable | Why |
|---|---|
| `POSTGRES_PASSWORD` | Compose refuses to start without it. No default password is shipped. |
| `SUPPLYGRAPH_ADMIN_PASSWORD` | Creates the first administrator (`admin`, 8+ characters). Without it nobody can sign in. |
| `SPRING_PROFILES_ACTIVE` | `prod` for a real deployment (starts empty). `prod,demo` for a public demo with sample data. |
| `SUPPLYGRAPH_DEMO_PASSWORD` | Only with `prod,demo`: the shared password of the demo accounts. The app refuses to start without it. |

**Two ways to deploy**

1. *Real use:* `SPRING_PROFILES_ACTIVE=prod`. The app starts empty. Sign in as `admin`, open **Add your data**, load your own CSV files or generate a network.
2. *Public demo:* `SPRING_PROFILES_ACTIVE=prod,demo`. It loads the sample supply chain, incidents and history and creates accounts `admin`, `manager`, `procurement`, `production`, `logistics`, `quality`, `analyst`, `viewer`, all with `SUPPLYGRAPH_DEMO_PASSWORD`. Because those usernames are public, never put real data in a demo deployment.

**What the production profile switches off**

- Only `/actuator/health` and `/actuator/info` are exposed, with no detail. Metrics and Prometheus are not published.
- The **Database** page is off (`SUPPLYGRAPH_DB_BROWSER=true` turns it on; it shows raw rows to any signed-in user).
- The PostgreSQL port is bound to `127.0.0.1` in `docker-compose.yml`.

**Build note.** `docker compose up -d --build` runs `./mvnw -Pproduction package`, which builds the optimised Vaadin frontend bundle that `vaadin.productionMode=true` needs. That step downloads Node.js, so the build machine needs internet access. This has not been run end to end on the machine this repository was developed on (it was offline); run it once before relying on it.

**Put it behind HTTPS.** The app does not terminate TLS. Use a reverse proxy or your platform's load balancer; `forward-headers-strategy: framework` is already set.

---

### 3. Database Migration Runbook (Flyway)

Flyway manages migrations automatically at application startup.

#### Current Migration History:
- `V1__init_schema.sql`: Workspaces, baseline tables.
- `V2__digital_twin_schema.sql`: Digital twin entities, nodes, edges, BOM, inventory ledgers.
- `V3__phase4_schema.sql`: Disruptions, impact assessments, recovery plans, simulations, optimization runs.
- `V4__phase5_schema.sql`: Operational events, monitoring rules, risk alerts, impact reassessments, replanning runs.
- `V5__phase6_schema.sql`: Enterprise user profiles, immutable audit logs, provenance records, research benchmark trials.

#### Manual Migration Check via CLI:
```bash
./mvnw -pl supplygraph-adapter-persistence flyway:info
```

---

### 4. Backup & Restore Procedures

#### Database Backup (PostgreSQL):
```bash
docker exec -t supplygraph-postgres pg_dump -U postgres -d supplygraph -F c -b -v -f /tmp/supplygraph_backup.dump
docker cp supplygraph-postgres:/tmp/supplygraph_backup.dump ./supplygraph_backup_$(date +%Y%m%d).dump
```

#### Database Restoration:
```bash
docker cp ./supplygraph_backup_20261117.dump supplygraph-postgres:/tmp/restore.dump
docker exec -t supplygraph-postgres pg_restore -U postgres -d supplygraph --clean --if-exists -v /tmp/restore.dump
```

---

### 5. Health, Readiness & Observability Endpoints

SupplyGraph integrates Spring Boot Actuator:
- **Liveness & Readiness Probe:** `GET /actuator/health`
- **Application Metrics:** `GET /actuator/metrics`
- **Runtime Thread Dump:** `GET /actuator/threaddump`

Inside the UI:
- **System Health View:** Navigate to `http://localhost:8080/health` to monitor uptime, heap usage, and security status.
- **Audit Governance View:** Navigate to `http://localhost:8080/audit-log` for real-time cryptographic audit trail inspection.
