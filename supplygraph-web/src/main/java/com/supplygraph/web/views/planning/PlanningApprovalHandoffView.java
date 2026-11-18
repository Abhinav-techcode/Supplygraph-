package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.PlanningScenarioService;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning/approval", layout = MainLayout.class)
@PageTitle("Planning Approval Handoff | SupplyGraph")
public class PlanningApprovalHandoffView extends PlanningViewBase {

    private final PlanningScenarioService scenarioService;

    public PlanningApprovalHandoffView(PlanningScenarioService scenarioService) {
        super("Planning Approval & Governed Execution Handoff", "Mandatory human review and binding authorization for operational supply chain plans.");
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();
        List<PlanningScenarioComparison> pendingScenarios = scenarioService.listScenarios(ws);

        UserProfile currentUser = SecurityContext.getCurrentUser();
        boolean hasPermission = currentUser != null && currentUser.role().hasPermission(Permission.PLAN_APPROVE);

        if (!hasPermission) {
            Span warning = new Span("HUMAN GOVERNANCE GUARD: Your active role lacks PLAN_APPROVE permission. Only authorized Supply Chain Managers or Administrators can sign-off operational plans.");
            warning.getElement().getThemeList().add("badge error");
            warning.getStyle().set("padding", "var(--lumo-space-m)").set("font-size", "var(--lumo-font-size-m)");
            add(warning);
        }

        VerticalLayout form = new VerticalLayout();
        form.addClassNames("border", "p-m", "rounded-m");
        form.setWidth("600px");

        ComboBox<PlanningScenarioComparison> select = new ComboBox<>("Select Plan Candidate for Sign-off", pendingScenarios);
        select.setItemLabelGenerator(s -> s.scenarioId().value() + " - " + s.scenarioName() + " (" + s.status().name() + ")");
        select.setWidthFull();

        TextArea rationaleField = new TextArea("Executive Sign-off Rationale & Notes");
        rationaleField.setWidthFull();
        rationaleField.setPlaceholder("State executive justification, risk mitigations reviewed, and approved budget allocation...");
        rationaleField.setValue("Reviewed trade-offs: cost within approved quarterly buffer envelope, 98%+ service level verified.");

        Button approveBtn = new Button("Authorize & Release Plan to Execution", VaadinIcon.CHECK_CIRCLE.create(), e -> {
            if (select.getValue() == null) {
                Notification.show("Please select a planning scenario candidate.", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (currentUser == null) {
                Notification.show("Security context has no authenticated principal.", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                PlanningScenarioComparison approved = scenarioService.approvePlan(
                        select.getValue().scenarioId(),
                        currentUser,
                        rationaleField.getValue()
                );
                Notification.show("Plan " + approved.scenarioId().value() + " APPROVED and released! Audit recorded.", 4000, Notification.Position.TOP_END);
                getUI().ifPresent(ui -> ui.navigate("planning"));
            } catch (SecurityException secEx) {
                Notification.show("Access Denied: " + secEx.getMessage(), 4000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Approval failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        approveBtn.getElement().getThemeList().add("success primary");
        approveBtn.setEnabled(hasPermission);

        form.add(new H3("Plan Sign-off Form"), select, rationaleField, approveBtn);
        add(form);
    }
}

