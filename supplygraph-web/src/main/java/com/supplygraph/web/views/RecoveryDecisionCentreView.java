package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.PlanComparisonMetrics;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

/**
 * Recovery Decision Centre View.
 * Capstone governance view presenting multi-alternative candidate recovery plans (Plan Alpha, Beta, Gamma),
 * displaying ConcurGuard conflict statuses, validating stale-state preconditions, and recording audited human approvals.
 */
@Route(value = "decision-centre", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Recovery Decision Centre")
public class RecoveryDecisionCentreView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final RecoveryPlanningApplicationService recoveryService;

    private final ComboBox<Disruption> disruptionSelector = new ComboBox<>();
    private final VerticalLayout plansContainer = new VerticalLayout();
    private final VerticalLayout auditContainer = new VerticalLayout();

    public RecoveryDecisionCentreView() {
        this(null, null);
    }

    @Autowired
    public RecoveryDecisionCentreView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) RecoveryPlanningApplicationService recoveryService
    ) {
        this.disruptionService = disruptionService;
        this.recoveryService = recoveryService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildControlBar();
        add(plansContainer, auditContainer);

        loadPlans();
    }

    private void buildHeader() {
        H2 title = new H2("Recovery Decision Centre & Human Executive Governance");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph desc = new Paragraph(
                "Multi-alternative recovery plan evaluation centre. AI agents and simulation engines propose alternatives, "
                        + "ConcurGuard detects conflicting resource commitments and stale-state drift, and human decision-makers "
                        + "grant explicit, audited approval before execution."
        );
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(title, desc);
    }

    private void buildControlBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        disruptionSelector.setLabel("Active Disruption Incident");
        disruptionSelector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        disruptionSelector.setWidth("450px");

        if (disruptionService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<Disruption> disruptions = disruptionService.listDisruptions(wsId);
            disruptionSelector.setItems(disruptions);
            if (!disruptions.isEmpty()) {
                disruptionSelector.setValue(disruptions.getFirst());
            }
        }
        disruptionSelector.addValueChangeListener(e -> loadPlans());

        Button btnGenerate = new Button("Generate Candidate Alternatives", VaadinIcon.MAGIC.create(), e -> generateAlternatives());
        btnGenerate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(disruptionSelector, btnGenerate);
        add(bar);
    }

    private void generateAlternatives() {
        Disruption d = disruptionSelector.getValue();
        if (d == null) {
            Notification.show("Please select an active disruption incident", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (recoveryService == null) {
            Notification.show("Recovery service unavailable", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        try {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            recoveryService.generateDefaultAlternatives(d.id(), wsId);
            loadPlans();
            Notification.show("Generated candidate recovery alternatives (Plan Alpha, Beta, Gamma)", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Generation error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadPlans() {
        plansContainer.removeAll();
        auditContainer.removeAll();

        if (recoveryService == null) {
            return;
        }

        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        List<RecoveryPlan> plans = recoveryService.listPlans(wsId);

        Disruption selectedDisruption = disruptionSelector.getValue();
        if (selectedDisruption != null) {
            plans = plans.stream()
                    .filter(p -> p.disruptionId().equals(selectedDisruption.id()))
                    .toList();
        }

        if (plans.isEmpty()) {
            plansContainer.add(new Paragraph("No candidate recovery plans generated yet. Click 'Generate Candidate Alternatives' above."));
            return;
        }

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        for (RecoveryPlan plan : plans) {
            cardsLayout.add(createPlanCard(plan));
        }

        plansContainer.add(cardsLayout);

        // Check for any approved plan to render audit card
        plans.stream()
                .filter(p -> p.status() == RecoveryPlanStatus.APPROVED && p.approval() != null)
                .findFirst()
                .ifPresent(this::renderApprovalAudit);
    }

    private VerticalLayout createPlanCard(RecoveryPlan plan) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("33%");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-shadow", "0 4px 8px rgba(0,0,0,0.06)");

        // 1. Header with Plan Name and Status Badge
        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 name = new H3(plan.planName());
        name.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-l)");

        Span statusBadge = new Span(plan.status().name());
        String theme = switch (plan.status()) {
            case APPROVED -> "badge success";
            case PENDING_APPROVAL -> "badge warning";
            case REJECTED -> "badge error";
            default -> "badge";
        };
        statusBadge.getElement().getThemeList().add(theme);
        top.add(name, statusBadge);

        Paragraph desc = new Paragraph(plan.description());
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        // 2. ConcurGuard Validation Badge
        Span concurBadge;
        if (plan.concurGuardValidated() && plan.concurGuardConflictIds().isEmpty()) {
            concurBadge = new Span("🛡️ ConcurGuard: Validated (No Conflicts)");
            concurBadge.getElement().getThemeList().add("badge success");
        } else if (!plan.concurGuardConflictIds().isEmpty()) {
            concurBadge = new Span("⚠️ ConcurGuard: Conflicts Detected (" + plan.concurGuardConflictIds().size() + ")");
            concurBadge.getElement().getThemeList().add("badge error");
        } else {
            concurBadge = new Span("ℹ️ ConcurGuard: Pending Validation");
            concurBadge.getElement().getThemeList().add("badge");
        }

        // 3. Trade-off Metrics
        PlanComparisonMetrics m = plan.metrics();
        VerticalLayout metricsBox = new VerticalLayout();
        metricsBox.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-s)");

        metricsBox.add(
                new Span("Estimated Cost: $" + m.totalCost().toPlainString()),
                new Span("Recovery Duration: " + String.format("%.1f", m.recoveryDurationDays()) + " Days"),
                new Span("Service Level: " + String.format("%.1f%%", m.serviceLevelImpactPercentage())),
                new Span("Risk Score: " + String.format("%.2f", m.operationalRiskScore()))
        );

        // 4. Actions Grid (Mini)
        Grid<PlanAction> actionGrid = new Grid<>(PlanAction.class, false);
        actionGrid.addColumn(PlanAction::description).setHeader("Action").setAutoWidth(true);
        actionGrid.addColumn(a -> "$" + a.costEstimate().toPlainString()).setHeader("Cost").setAutoWidth(true);
        actionGrid.setItems(plan.actions());
        actionGrid.setAllRowsVisible(true);

        // 5. Governance Action Buttons
        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setWidthFull();
        actionsRow.setSpacing(true);

        Button btnValidate = new Button("Validate", VaadinIcon.SHIELD.create(), e -> {
            try {
                recoveryService.validatePlanWithConcurGuard(plan.id());
                Notification.show("Plan verified against ConcurGuard conflict detection", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadPlans();
            } catch (Exception ex) {
                Notification.show("ConcurGuard error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnValidate.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button btnApprove = new Button("Approve", VaadinIcon.CHECK.create(), e -> openApprovalDialog(plan));
        btnApprove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        actionsRow.add(btnValidate, btnApprove);

        card.add(top, desc, concurBadge, metricsBox, actionGrid, actionsRow);
        return card;
    }

    private void openApprovalDialog(RecoveryPlan plan) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Authorize Recovery Plan Approval");
        dialog.setWidth("520px");

        FormLayout form = new FormLayout();

        TextField approverField = new TextField("Executive Approver Identity");
        // The approver must be a known user so the approval stays traceable when the plan is later converted into an execution workflow.
        approverField.setValue(com.supplygraph.application.security.SecurityContext.getCurrentUser() != null
                ? com.supplygraph.application.security.SecurityContext.getCurrentUser().id().value() : "VP_SUPPLY_CHAIN_OPERATIONS");
        approverField.setRequired(true);

        TextArea rationaleField = new TextArea("Governance Rationale & Policy Reference");
        rationaleField.setValue("Authorized under Global Supply Continuity Policy Sec 4.2. Selected Pareto optimal path balancing cost and service level.");
        rationaleField.setRequired(true);

        form.add(approverField, rationaleField);
        form.setColspan(rationaleField, 2);

        Button btnConfirm = new Button("Confirm & Legally Authorize", e -> {
            if (approverField.isEmpty() || rationaleField.isEmpty()) {
                Notification.show("Approver Identity and Rationale are required.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                recoveryService.approvePlan(
                        plan.id(),
                        approverField.getValue(),
                        rationaleField.getValue()
                );
                Notification.show("Plan successfully APPROVED and locked with audited governance record!", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadPlans();
            } catch (Exception ex) {
                Notification.show("Approval failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(btnCancel, btnConfirm);
        dialog.open();
    }

    private void renderApprovalAudit(RecoveryPlan plan) {
        auditContainer.removeAll();

        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-success-color-10pct)")
                .set("border", "1px solid var(--lumo-success-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-l)");

        H3 title = new H3("Executive Approval Audit Record: " + plan.planName());
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-success-text-color)");

        var app = plan.approval();
        if (app != null) {
            card.add(
                    title,
                    new Paragraph("Authorized By: " + app.approvedBy()),
                    new Paragraph("Approval Timestamp: " + app.approvalTimestamp()),
                    new Paragraph("Revalidated State Version: " + app.revalidatedTwinVersion()),
                    new Paragraph("Governance Rationale: " + app.rationale())
            );
        }

        auditContainer.add(card);
    }
}

