# SupplyGraph — Agent Tool Execution Security Specification

**Document ID:** `SG-DOC-013`  
**Phase:** Phase 3 (Agentic AI Orchestration, ConcurGuard & Conflict-Aware Coordination)  
**Status:** Approved Security Specification  

---

## 1. Zero-Trust Security Imperative

AI agent architectures frequently suffer from vulnerability vectors such as prompt injection, unauthorized cross-tenant data access, uncontrolled tool looping, and unintended side-effects. In SupplyGraph, agents are strictly sandboxed through a centralized Java execution gateway: the `ToolExecutionBoundary`.

---

## 2. The Seven Security Check Gates

Before any tool is executed on behalf of an agent, the `ToolExecutionBoundary` enforces seven consecutive check gates:

```
 Incoming Tool Execution Request
               │
               ▼
 [ Gate 1: Workspace Boundary Check ]       ── Fail ──> SecurityException ("Cross-workspace violation")
               │ Pass
               ▼
 [ Gate 2: Agent Identity Validation ]      ── Fail ──> SecurityException ("Missing agent identity")
               │ Pass
               ▼
 [ Gate 3: Tool Allow-List Verification ]   ── Fail ──> SecurityException ("Unauthorized tool access")
               │ Pass
               ▼
 [ Gate 4: Invocation Rate-Limit Check ]    ── Fail ──> IllegalStateException ("Rate limit exceeded")
               │ Pass
               ▼
 [ Gate 5: Tool Registry Existence Check ]  ── Fail ──> IllegalArgumentException ("Unknown tool")
               │ Pass
               ▼
 [ Gate 6: Input Parameter Schema Check ]   ── Fail ──> ToolExecutionResult.failure ("Missing parameter")
               │ Pass
               ▼
 [ Gate 7: Read-Only Tool Execution ]       ── Always ──> Immutable DigitalTwinSnapshot inspection only
               │
               ▼
 [ Comprehensive Audit Trail Logging ]     ──> Appended to immutable audit trail
```

---

## 3. Detailed Check Specifications

### 3.1. Gate 1: Workspace Boundary Isolation
- **Rule:** The `request.workspaceId()` must strictly equal the `boundWorkspaceId` of the boundary session.
- **Enforcement:** If a rogue prompt or hijacked agent attempts to pass a different workspace ID, execution is aborted immediately with a `SecurityException`, and a failed audit record is written.

### 3.2. Gate 2: Explicit Typed Identity
- **Rule:** Every request must carry a non-null `AgentIdentity` identifying the invoking agent. Anonymous tool execution is strictly prohibited.

### 3.3. Gate 3: Role-Bound Tool Allow-List
- **Rule:** Each agent identity carries an immutable `Set<String> allowedTools`. The requested tool name must be explicitly contained in this set.
- **Example:** The `LogisticsAgent` cannot invoke `query_inventory`; attempting to do so triggers a `SecurityException`.

### 3.4. Gate 4: Per-Agent Rate Limiting
- **Rule:** To prevent unbounded recursion, infinite tool hallucination loops, or denial-of-service, the boundary tracks an `AtomicInteger` invocation count per agent ID.
- **Default Limit:** 50 tool invocations per investigation session (configurable per environment).
- **Enforcement:** Invocations exceeding the threshold throw an `IllegalStateException`.

### 3.5. Gate 5: Input Schema Validation
- **Rule:** Mandatory parameters are verified before tool execution. For instance, `QueryInventoryTool` verifies that `materialId` is present and non-blank; `QueryCapacityTool` verifies that `resourceId` is provided.

### 3.6. Gate 6: Read-Only Invariant
- **Rule:** Under no circumstances may an `AgentTool` expose a mutating method on `DigitalTwinState`, `InventoryLedger`, `BillOfMaterials`, or `SupplyChainGraph`. Tools only query point-in-time immutable `DigitalTwinSnapshot`s.

### 3.7. Gate 7: Immutable Audit Logging
- **Rule:** Every execution attempt (whether successful, rejected by security, or failed due to invalid arguments) appends an immutable `ToolAuditEntry` containing:
  - `timestamp`: Exact timestamp of the attempt
  - `toolName`: Invoked tool name
  - `agentId`: Identity of the calling agent
  - `workspaceId`: Target workspace
  - `success`: Boolean execution status
  - `errorMessage`: Sanitized error explanation if failed
- The audit trail is embedded into the final `RecoveryProposal` for human verification.
