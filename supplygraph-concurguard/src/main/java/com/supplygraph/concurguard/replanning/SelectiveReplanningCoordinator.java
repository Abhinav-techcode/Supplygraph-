package com.supplygraph.concurguard.replanning;

import com.supplygraph.concurguard.api.ArbitrationStatus;
import com.supplygraph.concurguard.arbitration.ArbitrationDecision;
import com.supplygraph.concurguard.arbitration.ConcurGuardArbitrator;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.conflict.ConcurGuardConflictDetector;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Selective Replanning Coordinator:
 * Isolates minimal affected subgraphs, preserves unaffected valid proposals,
 * coordinates targeted replanning, and enforces mathematical termination proofs.
 */
public class SelectiveReplanningCoordinator {

    public static final int DEFAULT_MAX_ITERATIONS = 5;

    private final ConcurGuardConflictDetector conflictDetector;
    private final ConcurGuardArbitrator arbitrator;
    private final int maxIterations;

    public SelectiveReplanningCoordinator(ConcurGuardConflictDetector conflictDetector,
                                         ConcurGuardArbitrator arbitrator,
                                         int maxIterations) {
        this.conflictDetector = Objects.requireNonNull(conflictDetector, "ConflictDetector must not be null");
        this.arbitrator = Objects.requireNonNull(arbitrator, "Arbitrator must not be null");
        this.maxIterations = maxIterations <= 0 ? DEFAULT_MAX_ITERATIONS : maxIterations;
    }

    public SelectiveReplanningCoordinator(ConcurGuardConflictDetector conflictDetector, ConcurGuardArbitrator arbitrator) {
        this(conflictDetector, arbitrator, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Executes the bounded selective replanning loop.
     *
     * @param initialIntents initial batch of agent proposals
     * @param snapshot authoritative digital twin snapshot
     * @param replanSupplier generator that produces replacement proposals for invalidated intents
     * @return final replanning result
     */
    public ReplanningResult coordinate(
            Collection<ActionIntent> initialIntents,
            DigitalTwinSnapshot snapshot,
            BiFunction<Set<IntentId>, Integer, List<ActionIntent>> replanSupplier
    ) {
        Map<IntentId, ActionIntent> currentPool = new LinkedHashMap<>();
        for (ActionIntent i : initialIntents) {
            currentPool.put(i.id(), i);
        }

        Set<String> observedConflictSignatures = new HashSet<>();
        Set<IntentId> initiallyPreserved = new LinkedHashSet<>();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            // 1. Run deterministic conflict detection
            List<Conflict> conflicts = conflictDetector.detectAllConflicts(currentPool.values(), snapshot);

            // 2. Run deterministic arbitration
            Map<IntentId, ArbitrationDecision> decisions = arbitrator.arbitrate(
                    currentPool.values(), conflicts, snapshot.version());

            // 3. Check if all intents in current pool are ALLOWed
            boolean allApproved = decisions.values().stream()
                    .allMatch(d -> d.outcome() == ArbitrationStatus.ALLOW);

            if (allApproved && conflicts.isEmpty()) {
                return ReplanningResult.successful(
                        new ArrayList<>(currentPool.values()), decisions, iteration, initiallyPreserved);
            }

            // 4. Identify directly invalidated root intents
            Set<IntentId> directlyInvalidated = new LinkedHashSet<>();
            for (ArbitrationDecision d : decisions.values()) {
                if (d.outcome() != ArbitrationStatus.ALLOW) {
                    directlyInvalidated.add(d.intentId());
                }
            }

            // 5. Compute minimal affected subgraph via transitive dependency closure
            IntentDependencyGraph depGraph = new IntentDependencyGraph(currentPool.values());
            Set<IntentId> totalInvalidated = depGraph.computeTransitiveDependents(directlyInvalidated);

            // 6. Partition pool into preserved vs invalidated
            Map<IntentId, ActionIntent> preservedIntents = new LinkedHashMap<>();
            for (Map.Entry<IntentId, ActionIntent> e : currentPool.entrySet()) {
                if (!totalInvalidated.contains(e.getKey())) {
                    preservedIntents.put(e.getKey(), e.getValue());
                }
            }
            if (iteration == 1) {
                initiallyPreserved.addAll(preservedIntents.keySet());
            }

            // 7. Termination Guard: No-Progress Detection (Identical Conflict Signature)
            String conflictSig = computeConflictSignature(conflicts);
            if (observedConflictSignatures.contains(conflictSig)) {
                return ReplanningResult.escalated(
                        new ArrayList<>(preservedIntents.values()), decisions, iteration,
                        preservedIntents.keySet(),
                        "ESCALATION_NO_PROGRESS: Identical conflict cycle detected during replanning iteration " + iteration);
            }
            observedConflictSignatures.add(conflictSig);

            // 8. Request new replacement proposals ONLY for invalidated work
            List<ActionIntent> replacementProposals = replanSupplier.apply(totalInvalidated, iteration);

            // Rebuild pool: Preserved valid intents + new targeted replacement proposals
            currentPool.clear();
            currentPool.putAll(preservedIntents);
            for (ActionIntent replacement : replacementProposals) {
                currentPool.put(replacement.id(), replacement);
            }
        }

        // Reached max iterations without convergence -> Explicit Escalation
        List<Conflict> finalConflicts = conflictDetector.detectAllConflicts(currentPool.values(), snapshot);
        Map<IntentId, ArbitrationDecision> finalDecisions = arbitrator.arbitrate(
                currentPool.values(), finalConflicts, snapshot.version());

        return ReplanningResult.escalated(
                new ArrayList<>(currentPool.values()), finalDecisions, maxIterations,
                initiallyPreserved,
                "ESCALATION_MAX_ITERATIONS_EXCEEDED: Reached iteration limit (" + maxIterations + ") without converging on a conflict-free plan");
    }

    private static String computeConflictSignature(List<Conflict> conflicts) {
        List<String> tokens = new ArrayList<>();
        for (Conflict c : conflicts) {
            tokens.add(c.type() + ":" + c.primaryIntentId().value() + ":" + c.resourceOrEntityId());
        }
        Collections.sort(tokens);
        return String.join(";", tokens);
    }
}
