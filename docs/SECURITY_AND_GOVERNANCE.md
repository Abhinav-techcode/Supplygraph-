# SupplyGraph — Security, Governance & Threat Model Specification

**Classification:** Enterprise Security Architecture Document  
**Version:** 1.0.0  
**Effective Date:** November 17, 2026  

---

### 1. Security Architecture Principles

SupplyGraph enforces defense-in-depth across six distinct architectural layers:
1. **Zero-Trust Tool Execution Boundary:** Autonomous AI agents operate in an unprivileged, read-only context and cannot invoke arbitrary commands or mutate databases.
2. **Server-Side Authorization (RBAC):** Every domain and application use case enforces permissions on the server; client UI components never make authoritative access decisions.
3. **Workspace & Tenant Isolation (IDOR Defense):** Repositories and service boundaries validate workspace ownership on every lookup and mutation.
4. **Append-Only Tamper-Evident Audit Logging:** Cryptographic SHA-256 signatures guarantee non-repudiation of all human approvals and twin state changes.
5. **Strict Data Provenance:** Non-authoritative AI agent proposals and simulations are strictly segregated from authoritative ERP/sensor telemetry.
6. **Resource Exhaustion Defense:** Token bucket rate limiters protect expensive OR-Tools solvers, simulations, and LLM endpoints.

---

### 2. Role-Based Access Control (RBAC) Specification

| Enterprise Role | Display Name | Granular Permissions Held |
| :--- | :--- | :--- |
| `SYSTEM_ADMIN` | System Administrator | All 10 permissions (Global workspace bypass) |
| `WORKSPACE_ADMIN` | Workspace Administrator | All 10 permissions (Scoped to assigned workspaces) |
| `SUPPLY_CHAIN_MANAGER` | Supply Chain Manager | `TWIN_MANAGE`, `DISRUPTION_CREATE`, `IMPACT_ASSESS`, `AGENT_INVESTIGATE`, `OPTIMIZATION_EXECUTE`, `PLAN_REVIEW`, `PLAN_APPROVE`, `CONFIG_ADMIN`, `AUDIT_VIEW` |
| `PROCUREMENT_MANAGER` | Procurement Manager | `DISRUPTION_CREATE`, `IMPACT_ASSESS`, `AGENT_INVESTIGATE`, `PLAN_REVIEW`, `AUDIT_VIEW` |
| `PRODUCTION_MANAGER` | Production Manager | `DISRUPTION_CREATE`, `IMPACT_ASSESS`, `AGENT_INVESTIGATE`, `PLAN_REVIEW`, `AUDIT_VIEW` |
| `LOGISTICS_MANAGER` | Logistics Manager | `DISRUPTION_CREATE`, `IMPACT_ASSESS`, `AGENT_INVESTIGATE`, `PLAN_REVIEW`, `AUDIT_VIEW` |
| `QUALITY_MANAGER` | Quality Manager | `DISRUPTION_CREATE`, `IMPACT_ASSESS`, `PLAN_REVIEW`, `AUDIT_VIEW` |
| `ANALYST` | Operations Analyst | `IMPACT_ASSESS`, `AGENT_INVESTIGATE`, `OPTIMIZATION_EXECUTE`, `PLAN_REVIEW`, `AUDIT_VIEW` |
| `VIEWER` | Executive Viewer | None (Strict read-only access) |

---

### 3. Cryptographic Audit Governance

All consequential lifecycle events generate an immutable audit log entry in the `audit_logs` table:
$$\text{Signature} = \text{SHA-256}(\text{id} \mathbin{\Vert} \text{userId} \mathbin{\Vert} \text{workspaceId} \mathbin{\Vert} \text{action} \mathbin{\Vert} \text{targetId} \mathbin{\Vert} v_{\text{prev}} \mathbin{\Vert} v_{\text{new}} \mathbin{\Vert} t \mathbin{\Vert} \text{rationale} \mathbin{\Vert} \text{conflictRef})$$

#### Immutable Operations:
- `PLAN_APPROVED`: Binding human manager approval of a recovery plan. Autonomous AI approval is strictly prohibited.
- `PLAN_REJECTED`: Formal human rejection of candidate proposals.
- `DISRUPTION_CREATED`: Activation of incident and multi-tier cascading analysis.
- `DISRUPTION_RESOLVED`: Incident closure and restoration of baseline capacities.
- `DIGITAL_TWIN_ADVANCED`: StateVersion progression ($V_k \to V_{k+1}$).
- `REPLANNING_TRIGGERED`: Continuous intelligence triggering adaptive replanning.

---

### 4. Threat Model & Countermeasures

| Threat Vector | Attack Scenario | SupplyGraph Countermeasure |
| :--- | :--- | :--- |
| **Insecure Direct Object Reference (IDOR)** | Malicious actor queries or mutates entities belonging to another workspace by altering the workspaceId. | `WorkspaceAccessGuard.enforceWorkspaceAccess(wsId)` verifies actor's membership. Blocks unauthorized cross-tenant calls with `WorkspaceAccessDeniedException`. |
| **Prompt Injection / Jailbreak** | External event payload or prompt text contains instructions attempting to override agent instructions (e.g., "ignore previous instructions"). | `SanitizationAndSafetyGuard.validatePromptSafety()` scans text against injection signatures and throws `SecurityException`. |
| **Denial of Service via Heavy Compute** | Flooding simulation or OR-Tools optimization endpoints with concurrent requests to exhaust JVM CPU/memory. | `TokenBucketRateLimiter` enforces rate limiting (10 ops/min per client), throwing `RateLimitExceededException`. |
| **Malicious File Upload** | Attacker uploads oversized file or executable payload disguised as operational telemetry. | `SanitizationAndSafetyGuard.validateFileUpload()` enforces a 10 MB maximum size and an extension whitelist (`.csv`, `.json`, `.txt`, `.xml`). |
| **Audit Log Tampering** | Unauthorized DBA or insider modifies audit logs or approves plans retroactively. | Cryptographic SHA-256 signatures validated via `AuditGovernanceService.verifyIntegrity()`. Any mutation invalidates the hash. |
| **Autonomous Plan Execution** | AI agent attempts to auto-execute an unapproved recovery proposal. | Architecture boundary: recovery proposals are strictly non-authoritative (`AI_PROPOSAL`). Execution requires human approval with `PLAN_APPROVE` permission. |
