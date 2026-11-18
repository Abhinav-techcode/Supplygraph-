package com.supplygraph.application.execution;

import com.supplygraph.domain.model.execution.ExecutionTask;
import com.supplygraph.domain.model.execution.TaskType;
import com.supplygraph.domain.model.plan.PlanAction;

import java.util.*;

/**
 * Deterministic translation of recovery plan actions into tasks and dependencies. Pure function of the plan: same plan, same tasks.
 * Every plan action yields a short chain; procurement chains precede inventory/production tasks on the same material; a final
 * stakeholder notification depends on everything else.
 */
public final class WorkflowPlanner {
    private WorkflowPlanner() {}

    public static List<ExecutionTask> plan(String workflowId, List<PlanAction> actions, int maxAttempts) {
        List<ExecutionTask> tasks = new ArrayList<>();
        Map<String, List<String>> procurementByMaterial = new HashMap<>();
        int seq = 1;
        for (PlanAction a : actions) {
            List<TaskType> chain = chainFor(a);
            String previous = null;
            List<String> chainIds = new ArrayList<>();
            for (TaskType type : chain) {
                List<String> deps = new ArrayList<>();
                if (previous != null) deps.add(previous);
                boolean consumesMaterial = type == TaskType.ALLOCATE_INVENTORY || type == TaskType.CHANGE_PRODUCTION_SEQUENCE
                        || type == TaskType.REASSIGN_PRODUCTION_CAPACITY;
                String material = a.targetMaterialId() != null ? a.targetMaterialId().value() : null;
                if (consumesMaterial && material != null && previous == null) deps.addAll(procurementByMaterial.getOrDefault(material, List.of()));
                ExecutionTask t = ExecutionTask.create(workflowId, seq, type, describe(type, a), params(a), deps, expected(a, type), maxAttempts);
                tasks.add(t);
                previous = t.id();
                chainIds.add(t.id());
                seq++;
            }
            if (a.actionType() == com.supplygraph.domain.model.plan.PlanActionType.ALTERNATIVE_SUPPLIER
                    || a.actionType() == com.supplygraph.domain.model.plan.PlanActionType.PURCHASE_ORDER_EXPEDITE) {
                if (a.targetMaterialId() != null && previous != null) procurementByMaterial.computeIfAbsent(a.targetMaterialId().value(), k -> new ArrayList<>()).add(previous);
            }
        }
        List<String> all = tasks.stream().map(ExecutionTask::id).toList();
        tasks.add(ExecutionTask.create(workflowId, seq, TaskType.NOTIFY_STAKEHOLDERS, "Notify stakeholders of executed recovery actions", Map.of(), all, Map.of(), maxAttempts));
        return tasks;
    }

    static List<TaskType> chainFor(PlanAction a) {
        return switch (a.actionType()) {
            case ALTERNATIVE_SUPPLIER -> List.of(TaskType.REQUEST_ALTERNATE_QUOTATION, TaskType.REQUEST_SUPPLIER_CAPACITY_CONFIRMATION);
            case PURCHASE_ORDER_EXPEDITE -> List.of(TaskType.REQUEST_SUPPLIER_CAPACITY_CONFIRMATION, TaskType.EXPEDITE_PURCHASE_ORDER);
            case INVENTORY_REALLOCATION, EMERGENCY_BUFFER_RELEASE -> List.of(TaskType.ALLOCATE_INVENTORY);
            case PRODUCTION_RESCHEDULING -> List.of(TaskType.CHANGE_PRODUCTION_SEQUENCE);
            case EXPEDITED_FREIGHT -> "AIR".equalsIgnoreCase(a.parameters().getOrDefault("mode", "")) || "SEA".equalsIgnoreCase(a.parameters().getOrDefault("mode", ""))
                    ? List.of(TaskType.CHANGE_TRANSPORT_MODE) : List.of(TaskType.REROUTE_SHIPMENT);
            case CAPACITY_REASSIGNMENT -> List.of(TaskType.REASSIGN_PRODUCTION_CAPACITY);
        };
    }

    private static String describe(TaskType t, PlanAction a) { return t + " — " + a.description(); }

    private static Map<String, String> params(PlanAction a) {
        Map<String, String> p = new LinkedHashMap<>(a.parameters());
        p.put("planActionId", a.actionId());
        p.put("planActionType", a.actionType().name());
        if (a.targetNodeId() != null) p.put("targetNodeId", a.targetNodeId().value());
        if (a.targetMaterialId() != null) p.put("targetMaterialId", a.targetMaterialId().value());
        p.put("quantity", a.quantity().toPlainString());
        p.put("unit", a.unitOfMeasure());
        return p;
    }

    private static Map<String, String> expected(PlanAction a, TaskType t) {
        Map<String, String> e = new LinkedHashMap<>();
        if (t == TaskType.ALLOCATE_INVENTORY || t == TaskType.EXPEDITE_PURCHASE_ORDER) e.put("quantity", a.quantity().toPlainString());
        return e;
    }
}
