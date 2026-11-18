package com.supplygraph.domain.model.execution;

import java.util.*;

/** Dependency graph over tasks: validation, cycle detection (Kahn), runnable-set evaluation and failure propagation. */
public final class TaskGraph {
    public static final class CycleException extends RuntimeException {
        private final Set<String> members;
        public CycleException(Set<String> members) { super("Task dependency cycle involving: " + new TreeSet<>(members)); this.members = Set.copyOf(members); }
        public Set<String> members() { return members; }
    }

    private final Map<String, ExecutionTask> tasks = new LinkedHashMap<>();

    public TaskGraph(Collection<ExecutionTask> list) {
        for (ExecutionTask t : list) if (tasks.put(t.id(), t) != null) throw new IllegalArgumentException("Duplicate task id " + t.id());
        for (ExecutionTask t : list) {
            for (String d : t.dependsOn()) {
                if (!tasks.containsKey(d)) throw new IllegalArgumentException("Task " + t.id() + " depends on unknown task " + d);
                if (d.equals(t.id())) throw new CycleException(Set.of(t.id()));
            }
            if (t.conditionTaskId() != null && !tasks.containsKey(t.conditionTaskId()))
                throw new IllegalArgumentException("Task " + t.id() + " conditions on unknown task " + t.conditionTaskId());
        }
    }

    /** Topological order (stable by sequence); throws {@link CycleException} naming the tasks on/after the cycle. */
    public List<String> topologicalOrder() {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> out = new HashMap<>();
        for (ExecutionTask t : tasks.values()) {
            indegree.putIfAbsent(t.id(), 0);
            for (String d : edges(t)) { out.computeIfAbsent(d, k -> new ArrayList<>()).add(t.id()); indegree.merge(t.id(), 1, Integer::sum); }
        }
        PriorityQueue<ExecutionTask> queue = new PriorityQueue<>(Comparator.comparingInt(ExecutionTask::sequence));
        indegree.forEach((id, n) -> { if (n == 0) queue.add(tasks.get(id)); });
        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            ExecutionTask t = queue.poll();
            order.add(t.id());
            for (String next : out.getOrDefault(t.id(), List.of())) if (indegree.merge(next, -1, Integer::sum) == 0) queue.add(tasks.get(next));
        }
        if (order.size() != tasks.size()) {
            Set<String> stuck = new HashSet<>(tasks.keySet());
            stuck.removeAll(order);
            throw new CycleException(stuck);
        }
        return order;
    }

    /** PENDING tasks (or BLOCKED ones whose predecessors recovered, or READY ones reopened after reconciliation) whose predecessors are all COMPLETED and whose condition holds. */
    public List<ExecutionTask> runnable() {
        List<ExecutionTask> ready = new ArrayList<>();
        for (ExecutionTask t : tasks.values()) {
            if (t.status() != TaskStatus.PENDING && t.status() != TaskStatus.BLOCKED && t.status() != TaskStatus.READY) continue;
            if (!dependenciesSatisfied(t)) continue;
            if (t.conditionTaskId() != null && tasks.get(t.conditionTaskId()).status() != t.conditionOutcome()) continue;
            ready.add(t);
        }
        ready.sort(Comparator.comparingInt(ExecutionTask::sequence));
        return ready;
    }

    /** Tasks whose condition can no longer be met (referenced task is terminal with a different outcome). */
    public List<ExecutionTask> skippable() {
        List<ExecutionTask> out = new ArrayList<>();
        for (ExecutionTask t : tasks.values()) {
            if (t.status() != TaskStatus.PENDING || t.conditionTaskId() == null) continue;
            ExecutionTask c = tasks.get(t.conditionTaskId());
            if (c.status().isTerminal() && c.status() != t.conditionOutcome()) out.add(t);
        }
        return out;
    }

    /** PENDING tasks that can never run because a mandatory predecessor is a dead end (FAILED/BLOCKED/CANCELLED/SKIPPED). Condition predecessors are exempt. */
    public List<ExecutionTask> toBlock() {
        List<ExecutionTask> out = new ArrayList<>();
        for (ExecutionTask t : tasks.values()) {
            if (t.status() != TaskStatus.PENDING) continue;
            for (String d : t.dependsOn()) {
                ExecutionTask dep = tasks.get(d);
                boolean conditionEdge = d.equals(t.conditionTaskId());
                if (!conditionEdge && dep.status().isDeadEnd()) { out.add(t); break; }
            }
        }
        return out;
    }

    /** Transitive downstream task ids of a task (used by the dependency explorer). */
    public Set<String> downstreamOf(String taskId) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>(List.of(taskId));
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (ExecutionTask t : tasks.values()) if (edges(t).contains(cur) && seen.add(t.id())) stack.push(t.id());
        }
        return seen;
    }

    private boolean dependenciesSatisfied(ExecutionTask t) {
        for (String d : t.dependsOn()) {
            ExecutionTask dep = tasks.get(d);
            if (d.equals(t.conditionTaskId())) { if (!dep.status().isTerminal()) return false; }   // condition predecessor only has to be finished
            else if (!dep.status().isSuccess()) return false;
        }
        return true;
    }

    private static Set<String> edges(ExecutionTask t) {
        Set<String> e = new LinkedHashSet<>(t.dependsOn());
        if (t.conditionTaskId() != null) e.add(t.conditionTaskId());
        return e;
    }
}
