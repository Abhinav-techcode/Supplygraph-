# SupplyGraph Phase 4 — Monte Carlo Risk Analysis Engine

## 1. Architectural Overview

The **Monte Carlo Uncertainty & Risk Analysis Engine** resides in `supplygraph-simulation-engine`. It evaluates disruption severity and recovery plan robustness under operational stochasticity. Rather than relying on deterministic assumptions, the engine runs repeated independent simulations sampled from parameterized statistical distributions.

```
+-------------------------------------------------------------------------------+
|                    Monte Carlo Uncertainty Simulation Engine                  |
|                                                                               |
|   +-----------------------+      +------------------+      +---------------+  |
|   | MonteCarloConfig      | ---> | Replication Loop | ---> | Aggregator    |  |
|   | Replications & Seed   |      | (Seeded Runs)    |      | (P90/P95/Var) |  |
|   +-----------------------+      +------------------+      +---------------+  |
|               |                           |                        |          |
|               v                           v                        v          |
|   +-----------------------+      +------------------+      +---------------+  |
|   | Parameter Dist.       |      | Isolated State   |      | MonteCarlo    |  |
|   | Normal, Triang, Unif  |      | Execution Clone  |      | SummaryResult |  |
|   +-----------------------+      +------------------+      +---------------+  |
+-------------------------------------------------------------------------------+
```

## 2. Parameterized Probability Distributions

The engine defines a clean, zero-dependency statistical abstraction `ProbabilityDistribution`:
- `sample(Random random)`: Generates a bounded or unbounded double sample.
- `mean()`: Expected value of the distribution.
- `variance()`: Distribution variance.

### Implemented Distribution Types:
1. **Normal Distribution (`NormalDistribution`)**:
   - Parameters: Mean ($\mu$), Standard Deviation ($\sigma$).
   - Generation: Box-Muller transformation via `java.util.Random.nextGaussian()`.
   - Used for: Production cycle times, continuous equipment degradation, energy demand fluctuations.

2. **Triangular Distribution (`TriangularDistribution`)**:
   - Parameters: Lower bound ($a$), Most likely mode ($c$), Upper bound ($b$), where $a \le c \le b$.
   - Generation: Inverse cumulative distribution transform.
   - Used for: Logistics transit delays, customs clearance latencies, repair duration estimates.

3. **Uniform Distribution (`UniformDistribution`)**:
   - Parameters: Min ($min$), Max ($max$).
   - Generation: Linear scaling of standard pseudorandom variates.
   - Used for: Non-correlated vendor lead-time jitter, ambient environmental factors.

## 3. Stochastic Replication & Isolation

- **Seed Reproducibility**: Each experiment accepts an optional integer `seed`. If supplied, identical pseudorandom seeds yield mathematically bit-identical replication series.
- **State Immutability**: Each replication clones the baseline `DigitalTwinSnapshot`. Disruption impacts and candidate recovery plans are evaluated in isolation without state bleed across replications.
- **Fail-Safe Convergence**: Validated over $N \in [10, 10000]$ iterations with execution timeout boundaries to prevent infinite replication loops.

## 4. Statistical Metrics Contract (`MonteCarloSummaryResult`)

The output aggregates replication results into an immutable summary:
- `replications`: Total iteration count.
- `meanDelaySeconds`, `medianDelaySeconds`, `p90DelaySeconds`, `p95DelaySeconds`.
- `stockoutProbability`: Fraction of replications experiencing zero-inventory stockouts ($[0.0, 1.0]$).
- `slaComplianceRate`: Probability of meeting customer SLA deadlines.
- `meanCost`, `p95Cost`: Economic exposure under stochastic conditions.
- `individualRuns`: Detailed sample metrics per replication for histogram and percentile rendering.

