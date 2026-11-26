package com.supplygraph.web.views.execution;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.application.execution.RecoveryPlanConversionService;
import com.supplygraph.application.execution.WorkflowPlanner;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.execution.ExecutionTask;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.execution.TaskGraph;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "execution-designer", layout = MainLayout.class)
@PageTitle("Execution Workflow Designer | ORBIX")
public class ExecutionWorkflowDesignerView extends ExecutionViewBase {
    private final RecoveryPlanConversionService conversion;
    private final ComboBox<RecoveryPlan> planBox = new ComboBox<>("Approved plan");
    private final Grid<ExecutionTask> preview = new Grid<>();
    private final Paragraph summary = new Paragraph("Select an approved plan to preview the tasks and dependencies the deterministic planner would generate.");

    public ExecutionWorkflowDesignerView(ExecutionQueryService queries, RecoveryPlanningApplicationService planning, RecoveryPlanConversionService conversion) {
        super(queries, "Execution Workflow Designer", "Preview the deterministic task chain for an approved plan, then create the workflow. Tasks are generated, not hand-edited, so execution stays reproducible.");
        this.conversion = conversion;
        planBox.setItemLabelGenerator(p -> p.id().value() + " · " + p.planName());
        planBox.setWidth("40em");
        planBox.setItems(UiSupport.call(() -> planning.listPlans(UiSupport.workspace()), List.<RecoveryPlan>of()).stream().filter(p -> p.status() == RecoveryPlanStatus.APPROVED || p.status() == RecoveryPlanStatus.DRAFT).limit(40).toList());
        planBox.getListDataView().getItems().findFirst().ifPresent(planBox::setValue);
        planBox.addValueChangeListener(e -> showPreview(e.getValue()));
        preview.addColumn(ExecutionTask::sequence).setHeader("#").setAutoWidth(true);
        preview.addColumn(ExecutionTask::type).setHeader("Task type").setAutoWidth(true);
        preview.addColumn(t -> t.sensitive() ? "yes (second approval)" : "no").setHeader("Sensitive").setAutoWidth(true);
        preview.addColumn(t -> t.dependsOn().isEmpty() ? "—" : String.join(", ", t.dependsOn())).setHeader("Depends on").setAutoWidth(true);
        preview.addColumn(t -> t.type().compensable() ? t.type().compensation() : "none (" + (t.type().compensation() == null ? "irreversible" : "cannot be reversed") + ")").setHeader("Compensation").setAutoWidth(true);
        preview.addColumn(ExecutionTask::description).setHeader("Description").setAutoWidth(true);
        preview.setSizeFull();
        Button create = new Button("Create workflow from plan", e -> {
            RecoveryPlan p = planBox.getValue();
            if (p == null) { UiSupport.error(new ValidationException("Select a plan first")); return; }
            ExecutionWorkflow w = UiSupport.call(() -> conversion.convert(p.id().value()), null);
            if (w != null) summary.setText("Workflow " + w.id() + " is " + w.status() + (w.statusReason() != null ? ": " + w.statusReason() : ". Go to Execution Approvals."));
        });
        create.addThemeName("primary");
        body(new HorizontalLayout(planBox, create), summary, preview);
    }

    private void showPreview(RecoveryPlan p) {
        if (p == null) { preview.setItems(List.of()); return; }
        List<ExecutionTask> tasks = WorkflowPlanner.plan("PREVIEW", p.actions(), 3);
        preview.setItems(tasks);
        try {
            summary.setText(tasks.size() + " task(s). Execution order: " + String.join(" → ", new TaskGraph(tasks).topologicalOrder()).replace("PREVIEW-", ""));
        } catch (TaskGraph.CycleException e) {
            summary.setText("Cycle detected: " + e.getMessage());
        }
    }
}
