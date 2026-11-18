package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.*;

@Route(value = "task-dependencies", layout = MainLayout.class)
@PageTitle("Task Dependency Explorer | SupplyGraph")
public class TaskDependencyExplorerView extends ExecutionViewBase {
    private final ComboBox<ExecutionWorkflow> selector = workflowSelector("Workflow");
    private final Html graph = new Html("<div></div>");
    private final Paragraph summary = new Paragraph("Select a workflow to see its dependency graph.");
    private final Grid<ExecutionTask> grid = new Grid<>();
    private final com.vaadin.flow.component.orderedlayout.VerticalLayout graphHolder = new com.vaadin.flow.component.orderedlayout.VerticalLayout();

    public TaskDependencyExplorerView(ExecutionQueryService queries) {
        super(queries, "Task Dependency Explorer", "Graph of tasks and the predecessors each one waits for. Colour shows task status; a task only runs when every predecessor is COMPLETED.");
        selector.addValueChangeListener(e -> show(e.getValue()));
        grid.addColumn(ExecutionTask::id).setHeader("Task").setAutoWidth(true);
        grid.addColumn(ExecutionTask::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(ExecutionTask::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(t -> t.dependsOn().isEmpty() ? "—" : String.join(", ", t.dependsOn())).setHeader("Depends on").setAutoWidth(true);
        grid.addColumn(t -> t.conditionTaskId() == null ? "" : "only if " + t.conditionTaskId() + " " + t.conditionOutcome()).setHeader("Condition").setAutoWidth(true);
        grid.addColumn(ExecutionTask::lastError).setHeader("Reason").setAutoWidth(true);
        grid.setHeight("18em");
        graphHolder.setPadding(false);
        graphHolder.add(graph);
        graphHolder.getStyle().set("overflow", "auto");
        body(selector, summary, graphHolder, grid);
    }

    private void show(ExecutionWorkflow selected) {
        if (selected == null) return;
        ExecutionWorkflow w = queries.get(selected.id());
        TaskGraph g = w.graph();
        grid.setItems(w.tasks());
        summary.setText("Execution order: " + String.join(" → ", g.topologicalOrder()).replace(w.id() + "-", ""));
        graphHolder.removeAll();
        graphHolder.add(new Html(svg(w)));
    }

    /** Layered layout: a task's column is its longest predecessor chain. Pure server-side SVG, no JS stack. */
    static String svg(ExecutionWorkflow w) {
        Map<String, Integer> depth = new HashMap<>();
        for (String id : w.graph().topologicalOrder()) {
            ExecutionTask t = w.task(id).orElseThrow();
            int d = 0;
            for (String dep : t.dependsOn()) d = Math.max(d, depth.get(dep) + 1);
            if (t.conditionTaskId() != null) d = Math.max(d, depth.get(t.conditionTaskId()) + 1);
            depth.put(id, d);
        }
        Map<Integer, List<ExecutionTask>> columns = new TreeMap<>();
        for (ExecutionTask t : w.tasks()) columns.computeIfAbsent(depth.get(t.id()), k -> new ArrayList<>()).add(t);
        int boxW = 190, boxH = 46, gapX = 70, gapY = 22;
        Map<String, int[]> pos = new HashMap<>();
        int maxRows = 0;
        for (var e : columns.entrySet()) {
            int row = 0;
            for (ExecutionTask t : e.getValue()) { pos.put(t.id(), new int[]{20 + e.getKey() * (boxW + gapX), 20 + row * (boxH + gapY)}); row++; }
            maxRows = Math.max(maxRows, row);
        }
        int width = 40 + columns.size() * (boxW + gapX), height = 40 + maxRows * (boxH + gapY);
        StringBuilder sb = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='" + width + "' height='" + height + "' role='img' aria-label='Task dependency graph'>")
                .append("<defs><marker id='arr' markerWidth='8' markerHeight='8' refX='7' refY='4' orient='auto'><path d='M0,0 L8,4 L0,8 z' fill='#6b7280'/></marker></defs>");
        for (ExecutionTask t : w.tasks()) {
            int[] to = pos.get(t.id());
            Set<String> from = new LinkedHashSet<>(t.dependsOn());
            if (t.conditionTaskId() != null) from.add(t.conditionTaskId());
            for (String d : from) {
                int[] fr = pos.get(d);
                sb.append("<line x1='").append(fr[0] + boxW).append("' y1='").append(fr[1] + boxH / 2).append("' x2='").append(to[0] - 2).append("' y2='").append(to[1] + boxH / 2)
                        .append("' stroke='#6b7280' stroke-width='1.5' marker-end='url(#arr)'").append(d.equals(t.conditionTaskId()) ? " stroke-dasharray='5,3'" : "").append("/>");
            }
        }
        for (ExecutionTask t : w.tasks()) {
            int[] p = pos.get(t.id());
            String label = t.type().name().replace('_', ' ').toLowerCase();
            if (label.length() > 26) label = label.substring(0, 25) + "…";
            sb.append("<g><rect x='").append(p[0]).append("' y='").append(p[1]).append("' rx='6' width='").append(boxW).append("' height='").append(boxH)
                    .append("' fill='").append(color(t.status())).append("' stroke='#374151'/>")
                    .append("<text x='").append(p[0] + 8).append("' y='").append(p[1] + 19).append("' font-size='12' fill='#111827'>").append(escape(t.id().substring(t.id().lastIndexOf('-') + 1) + " " + label)).append("</text>")
                    .append("<text x='").append(p[0] + 8).append("' y='").append(p[1] + 36).append("' font-size='11' fill='#111827'>").append(t.status()).append(t.completedWithSimulatedEvidence() ? " [SIM]" : "").append("</text></g>");
        }
        return sb.append("</svg>").toString();
    }

    private static String color(TaskStatus s) {
        return switch (s) {
            case COMPLETED -> "#bbf7d0"; case AWAITING_ACK, IN_PROGRESS, READY -> "#bfdbfe"; case FAILED -> "#fecaca";
            case BLOCKED, RETRY_PENDING -> "#fde68a"; case RECONCILIATION_REQUIRED -> "#fed7aa"; case COMPENSATED -> "#e9d5ff"; default -> "#e5e7eb";
        };
    }
    private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"); }
}
