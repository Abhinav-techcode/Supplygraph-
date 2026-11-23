# SupplyGraph: how to operate it

## Start
```bash
./run-demo.sh                # PostgreSQL in Docker (default), http://localhost:8080
DB=h2 ./run-demo.sh          # in-memory database, no Docker (data is lost on restart)
```
Sign in at the welcome page. Local demo accounts (password `Demo!2026`): `manager`, `admin`, `procurement`, `production`, `logistics`, `quality`, `analyst`, `viewer`.

## Five-minute tour
1. **Welcome page** explains the product. Click **Sign in**.
2. **Home, Step 1 "Your data"**: *Load sample supply chain* (or *Import my own data (CSV)*).
3. **Network map**: click a facility to trace its supply path; red = disrupted.
4. **Specialist agents**: *Run all 8 agents*. The percentage is data completeness (facts found), not a probability.
5. **Simulation & risk**: *Run simulation + Monte Carlo*.
6. **Guided recovery**: seven steps (detect, impact, agents, options, ConcurGuard, approve, execute).
7. **System monitor** (Administration): live health checks, speed, memory, errors.

## Two-person approval
A workflow cannot be started by the person who created it. Create/approve as `manager`, approve the workflow as `admin`, start it as `logistics`.

Demo video: `docs/demo/SupplyGraph_demo.mp4`.

## Added later

**No data of your own?** Open *Add your data* and use *Generate and load*: choose an industry, region and size. City locations are real; company names, capacities and stock are invented; transit times and freight costs are estimated from distance. The demo seed also grows the sample into a 38-facility network with four live incidents and 150 generated telemetry events.

**Move things on the network map.** Drag a facility to reposition it (its lanes follow), drag the background to pan, use + / − to zoom, *Reset layout* to go back to the automatic layout.

**See the database.** *Administration > Database* shows the live PostgreSQL connection, every table with its row count, and the first 50 rows of any table (read-only; secret columns masked). To use your own tool: host `localhost`, port `5433`, database `supplygraph`, user `postgres` (demo container).

**Executing a recovery needs two different people.** In *Guided recovery* step 7, press *Create governed workflow* as `manager`. Sign out, sign in as `admin` (password Demo!2026), open *Execution > Approvals* and approve with a rationale. Sign back in as `manager`, reopen step 7 (the workflow is restored), press *Refresh status*, then *Start execution* and *Confirm the stock was moved*. A workflow that needs sensitive-task approval needs a third approver.
