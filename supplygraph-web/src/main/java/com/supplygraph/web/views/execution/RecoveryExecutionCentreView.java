package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.application.execution.RecoveryPlanConversionService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.web.init.DemoScenarioService;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "execution-centre", layout = MainLayout.class)
@PageTitle("Recovery Execution Centre | ORBIX")
public class RecoveryExecutionCentreView extends ExecutionViewBase {
    private final RecoveryPlanningApplicationService planning;
    private final RecoveryPlanConversionService conversion;
    private final DemoScenarioService demo;
    private final Grid<RecoveryPlan> plans = new Grid<>();
    private final Grid<ExecutionWorkflow> workflows = new Grid<>();
    private final Span noPlans = new Span("No approved recovery plans. Approve a plan in the Decision Centre first.");
    private final Span noWorkflows = new Span("No execution workflows yet.");

    public RecoveryExecutionCentreView(ExecutionQueryService queries, RecoveryPlanningApplicationService planning, RecoveryPlanConversionService conversion, DemoScenarioService demo) {
        super(queries, "Recovery Execution Centre", "Approved plans become governed execution workflows. Nothing executes without workflow approval and an authorized executor.");
        this.planning = planning;
        this.conversion = conversion;
        this.demo = demo;
        plans.addColumn(p -> p.id().value()).setHeader("Plan").setAutoWidth(true);
        plans.addColumn(RecoveryPlan::planName).setHeader("Name").setAutoWidth(true);
        plans.addColumn(p -> p.approval() == null ? "" : p.approval().approvedBy()).setHeader("Approved by").setAutoWidth(true);
        plans.addColumn(p -> p.actions().size()).setHeader("Actions").setAutoWidth(true);
        plans.addComponentColumn(p -> new Button("Create workflow", e -> {
            ExecutionWorkflow w = UiSupport.call(() -> conversion.convert(p.id().value()), null);
            if (w != null) { UiSupport.run(w.status() + (w.statusReason() != null ? " — " + w.statusReason() : " — awaiting approval"), () -> {}); refresh(); }
        })).setHeader("Action");
        plans.setHeight("14em");
        workflows.addColumn(ExecutionWorkflow::id).setHeader("Workflow").setAutoWidth(true);
        workflows.addColumn(ExecutionWorkflow::status).setHeader("Status").setAutoWidth(true);
        workflows.addColumn(ExecutionViewBase::mode).setHeader("Mode").setAutoWidth(true);
        workflows.addColumn(w -> w.tasks().size()).setHeader("Tasks").setAutoWidth(true);
        workflows.addColumn(ExecutionWorkflow::planId).setHeader("Plan").setAutoWidth(true);
        workflows.addColumn(w -> w.statusReason() == null ? "" : w.statusReason()).setHeader("Reason").setAutoWidth(true);
        workflows.addColumn(RecoveryExecutionCentreView::nextStep).setHeader("What to do next").setAutoWidth(true);
        workflows.setSizeFull();
        Button prepare = new Button("Prepare a demo plan (validate + approve as me)", e -> {
            java.util.Set<String> used = new java.util.HashSet<>();
            loadWorkflows().forEach(w -> used.add(w.planId()));
            if (UiSupport.run("A plan is approved and ready to convert into a workflow", () -> demo.prepareApprovedPlan(UiSupport.workspace(), used))) refresh();
        });
        prepare.addThemeName("primary");
        body(new com.vaadin.flow.component.html.Paragraph("Quick start: 1) click “Prepare a demo plan” (uses the real ConcurGuard validation and approval), 2) “Create workflow”, 3) follow the “What to do next” column — it tells you which role to switch to using the role selector in the header."),
                new HorizontalLayout(prepare, new Button("Refresh", e -> refresh())), new H3("Approved recovery plans"), noPlans, plans, new H3("Execution workflows"), noWorkflows, workflows);
        refresh();
    }

    static String nextStep(ExecutionWorkflow w) {
        return switch (w.status()) {
            case PENDING_APPROVAL -> w.hasApproval(com.supplygraph.domain.model.execution.WorkflowApproval.Scope.WORKFLOW)
                    ? "Switch role to SYSTEM_ADMIN → Execution Approvals → approve sensitive tasks (second, different approver)"
                    : "Switch role to WORKSPACE_ADMIN → Execution Approvals → Approve (the creator cannot approve)";
            case READY -> "Switch role to LOGISTICS_MANAGER → Live Execution Monitor → Start execution (approvers cannot execute)";
            case EXECUTING -> "Acknowledgements → record the (simulated) acknowledgement for each waiting task";
            case RECONCILIATION_REQUIRED -> "Reconciliation Workspace → confirm what really happened";
            case COMPENSATION_REQUIRED, PARTIALLY_COMPLETED -> "Compensation and Recovery Centre";
            case COMPLETED -> "Plan-versus-Actual Analytics → record measured results";
            case BLOCKED -> "Read the reason; a reassessment proposal may be waiting in the Closed-Loop dashboard";
            default -> "";
        };
    }

    private void refresh() {
        List<RecoveryPlan> approved = UiSupport.call(() -> planning.listPlans(UiSupport.workspace()), List.<RecoveryPlan>of()).stream().filter(p -> p.status() == RecoveryPlanStatus.APPROVED).toList();
        plans.setItems(approved); noPlans.setVisible(approved.isEmpty()); plans.setVisible(!approved.isEmpty());
        List<ExecutionWorkflow> wf = loadWorkflows();
        workflows.setItems(wf); noWorkflows.setVisible(wf.isEmpty()); workflows.setVisible(!wf.isEmpty());
    }
}
