# SupplyGraph Production Deployment Guide

**Target Audience:** DevOps Engineers, Cloud Infrastructure Architects, Site Reliability Engineers  
**Supported Platforms:** Docker Compose, Kubernetes, AWS ECS, GCP Cloud Run, Azure Container Instances

---

## 1. Prerequisites & System Requirements

- **Runtime:** Java 21 LTS (Eclipse Temurin 21 recommended)
- **Database:** PostgreSQL 16+ with SSL enabled
- **Memory:** Minimum 4GB RAM (8GB recommended for production simulation workloads)
- **CPU:** 4 Cores minimum
- **Disk:** 50GB NVMe SSD (for PostgreSQL persistence and ledger audit trails)

---

## 2. Environment Variables

| Variable Name | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://postgres.corp:5432/supplygraph` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `supplygraph_app` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | *[SECURE_SECRET]* |
| `DB_POOL_MAX` | Maximum HikariCP pool connections | `25` |
| `PORT` | Web application port | `8080` |
| `VAADIN_PRODUCTION_MODE` | Vaadin production bundle mode | `true` |
| `SUPPLYGRAPH_SECURITY_ENFORCE_RBAC` | Enforce server-side permissions | `true` |

---

## 3. Docker Production Deployment

### 3.1. Build Production Image
```bash
docker build -t supplygraph-enterprise:latest .
```

### 3.2. Deploy with Docker Compose
```bash
docker-compose -f docker-compose.yml up -d
```

### 3.3. Verify Container Health
```bash
curl -f http://localhost:8080/actuator/health
```
Expected response:
```json
{"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

## 4. Database Migrations & Versioning

- Migrations are managed automatically via Flyway (`db/migration/V1__*.sql` through `V16__*.sql`).
- In production, Flyway runs with `validate-on-migrate: true` and `baseline-on-migrate: false` to ensure schema integrity.
- Never manually execute DDL in production; always author Flyway migration scripts.

---

## 5. Backup & Disaster Recovery

### Daily Logical Backup:
```bash
pg_dump -U postgres -h <host> -Fc supplygraph > supplygraph_backup_$(date +%Y%m%d).dump
```

### Point-in-Time Recovery (PITR):
Ensure PostgreSQL WAL archiving is active. To restore:
```bash
pg_restore -U postgres -d supplygraph --clean supplygraph_backup_20261118.dump
```

