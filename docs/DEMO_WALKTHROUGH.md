# Demo Walkthrough (about 10 minutes, no PostgreSQL needed)

## Start
```bash
./run-demo.sh            # opens on http://localhost:8080  (use PORT=8090 ./run-demo.sh for another port)
```
It needs JDK 21 (the script finds it, or tells you what to install) and builds the project first (~1–2 minutes the first time; the first start also builds the Vaadin frontend, ~1 minute). Data is in memory (H2) and is re-seeded at every start. A red box at the top of the left menu is the **demo role switcher** — there is no login system, so you pick which role you are acting as. It is not authentication.

Everything below is **SIMULATED**: the sandbox connector and sandbox execution adapter contact no external system, and their results never change the digital twin.

## Part A — Integration runtime (role: SUPPLY_CHAIN_MANAGER, the default)
1. **Enterprise Integration → Connector Registry → Register connector.** Name it, pick category *TMS*, leave mode *SIMULATED* and adapter *sandbox*, keep “activate immediately” ticked → *Register*. The connector appears as ACTIVE.
2. **Synchronization** → pick the connector → *Synchronize now*. Five synthetic shipment records are received and reconciled. **Sync History** shows the run (labelled SIMULATED); **Operational Events** shows the resulting events.
3. **Connector Health** → *Test connectivity*. **Runtime Operations** shows rates computed from those runs ("unknown" where there is no data).
4. **Reconciliation** shows decisions per field. Conflicts (both sides changed) wait there for you to resolve with a rationale.
5. Webhooks (optional, command line): sign `"{unixSeconds}.{body}"` with HMAC-SHA256 and POST to `/api/webhooks/{connectorId}`; see `docs/CONNECTOR_DEVELOPER_GUIDE.md`.

## Part B — Governed execution
Open **Governed Execution → Recovery Execution Centre** (`/execution-centre`).
1. Click **Prepare a demo plan (validate + approve as me)**. This runs the real ConcurGuard validation and approval on a seeded candidate plan (if the seeded plans were built on an older twin version, it regenerates fresh alternatives, exactly as the Decision Centre button would).
2. Click **Create workflow** on the approved plan. The workflow appears as PENDING_APPROVAL; the **What to do next** column tells you the next role.
3. Switch the role (left-menu switcher) to **WORKSPACE_ADMIN** → **Execution Approvals** → type a rationale → *Approve*. (As SUPPLY_CHAIN_MANAGER, try it first: the server refuses — the creator cannot approve.) If the workflow has sensitive tasks, a second approval by a *different* role (**SYSTEM_ADMIN**, button “Approve sensitive tasks”) is required.
4. Switch to **LOGISTICS_MANAGER** → **Live Execution Monitor** → *Start execution*. (As WORKSPACE_ADMIN, try it first: refused, approvers cannot execute.) Tasks go to AWAITING_ACK — *accepted, not done* — and dependents wait.
5. Switch to **SUPPLY_CHAIN_MANAGER** → **Acknowledgements** → *Record acknowledgement* → *Task completed*. The next task is released; repeat until the workflow is COMPLETED. **Task Dependency Explorer** draws the graph; **Audit Timeline** shows every step with correlation id and SIMULATED labels.
6. **Plan-versus-Actual**: record a measured cost; because the workflow is simulated it is stored as SIMULATED and never triggers reassessment. **Closed-Loop Dashboard** shows policy and proposals.

## Try the failure paths
Failure behaviour is driven by the plan action parameter `sim.behavior` (see `docs/EXECUTION_ADAPTER_DEVELOPER_GUIDE.md`); the automated tests cover timeouts, retries, unknown outcomes, restart recovery, compensation and stale plans.
