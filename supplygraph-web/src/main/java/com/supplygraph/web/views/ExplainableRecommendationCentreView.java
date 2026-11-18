package com.supplygraph.web.views;

import com.supplygraph.application.explainability.ExplainableRecommendationService;
import com.supplygraph.domain.model.explainability.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
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

import java.util.List;

@Route(value = "recommendations-explainable", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Explainable Recovery Recommendations")
public class ExplainableRecommendationCentreView extends VerticalLayout {

    private final ExplainableRecommendationService recommendationService;
    private final Grid<ExplainableRecommendation> recommendationsGrid = new Grid<>();
    private final VerticalLayout detailsContainer = new VerticalLayout();

    public ExplainableRecommendationCentreView() {
        this(null);
    }

    @Autowired
    public ExplainableRecommendationCentreView(ExplainableRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Explainable Recovery Recommendations");
        Paragraph subtitle = new Paragraph("Transparent, evidence-backed recovery guidance with quantified trade-offs, constraint auditing, uncertainty parameters, and audited human approval.");

        configureGrid();
        configureDetails();

        add(header, subtitle, recommendationsGrid, detailsContainer);
        refreshData();
    }

    private void configureGrid() {
        recommendationsGrid.setHeight("250px");
        recommendationsGrid.addColumn(r -> r.id().value()).setHeader("Recommendation ID").setAutoWidth(true);
        recommendationsGrid.addColumn(ExplainableRecommendation::title).setHeader("Title").setAutoWidth(true);
        recommendationsGrid.addColumn(r -> "$" + r.expectedCostUsd()).setHeader("Expected Cost").setAutoWidth(true);
        recommendationsGrid.addColumn(r -> r.expectedRecoveryTimeHours() + " hrs").setHeader("Time to Recover").setAutoWidth(true);
        recommendationsGrid.addColumn(r -> r.serviceLevelRestorationPct() + "%").setHeader("Service Level").setAutoWidth(true);
        recommendationsGrid.addComponentColumn(r -> {
            Span badge = new Span(r.approved() ? "APPROVED (Human)" : "PENDING HUMAN APPROVAL");
            badge.getElement().getThemeList().add(r.approved() ? "badge success" : "badge warning");
            return badge;
        }).setHeader("Approval Status").setAutoWidth(true);

        recommendationsGrid.asSingleSelect().addValueChangeListener(event -> {
            ExplainableRecommendation selected = event.getValue();
            if (selected != null) {
                displayDetails(selected);
            }
        });
    }

    private void configureDetails() {
        detailsContainer.setSizeFull();
        detailsContainer.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        detailsContainer.getStyle().set("border-radius", "8px");
        detailsContainer.setPadding(true);
        detailsContainer.add(new Paragraph("Select a recommendation above to inspect evidence, evaluated constraints, trade-offs, and execute audited human approval."));
    }

    private void displayDetails(ExplainableRecommendation rec) {
        detailsContainer.removeAll();

        H3 title = new H3(rec.title() + " [" + rec.id().value() + "]");
        Paragraph summary = new Paragraph(rec.summary());

        // Actions
        H4 actionsHeader = new H4("Primary Recovery Actions");
        UnorderedList actionsList = new UnorderedList();
        for (String action : rec.primaryActions()) {
            actionsList.add(new ListItem(action));
        }

        // Evidence
        H4 evidenceHeader = new H4("Supporting Telemetry & Simulation Evidence");
        Grid<DecisionEvidence> evidenceGrid = new Grid<>();
        evidenceGrid.setHeight("160px");
        evidenceGrid.setItems(rec.evidenceList());
        evidenceGrid.addColumn(DecisionEvidence::title).setHeader("Signal Title").setAutoWidth(true);
        evidenceGrid.addColumn(DecisionEvidence::source).setHeader("Data Source").setAutoWidth(true);
        evidenceGrid.addColumn(e -> (int) (e.confidenceScore() * 100) + "%").setHeader("Confidence").setAutoWidth(true);
        evidenceGrid.addColumn(DecisionEvidence::summary).setHeader("Summary").setAutoWidth(true);

        // Constraints
        H4 constraintsHeader = new H4("Constraint Auditing");
        Grid<DecisionConstraint> constraintsGrid = new Grid<>();
        constraintsGrid.setHeight("160px");
        constraintsGrid.setItems(rec.constraintsEvaluated());
        constraintsGrid.addColumn(DecisionConstraint::code).setHeader("Code").setAutoWidth(true);
        constraintsGrid.addColumn(c -> c.type().name()).setHeader("Type").setAutoWidth(true);
        constraintsGrid.addComponentColumn(c -> {
            Span b = new Span(c.satisfied() ? "COMPLIANT" : "BREACHED");
            b.getElement().getThemeList().add(c.satisfied() ? "badge success" : "badge error");
            return b;
        }).setHeader("Compliance").setAutoWidth(true);
        constraintsGrid.addColumn(DecisionConstraint::violationImpact).setHeader("Evaluation Details").setAutoWidth(true);

        // Trade-Offs
        H4 tradeoffsHeader = new H4("Quantified Trade-Offs");
        Grid<DecisionTradeOff> tradeoffsGrid = new Grid<>();
        tradeoffsGrid.setHeight("150px");
        tradeoffsGrid.setItems(rec.tradeOffs());
        tradeoffsGrid.addColumn(DecisionTradeOff::dimension).setHeader("Dimension").setAutoWidth(true);
        tradeoffsGrid.addColumn(DecisionTradeOff::optionAValue).setHeader("Option A").setAutoWidth(true);
        tradeoffsGrid.addColumn(DecisionTradeOff::optionBValue).setHeader("Option B").setAutoWidth(true);
        tradeoffsGrid.addColumn(DecisionTradeOff::description).setHeader("Analysis").setAutoWidth(true);

        // Action Buttons
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button approveBtn = new Button("Approve Recommendation (Human Sign-off)", VaadinIcon.CHECK.create());
        approveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        approveBtn.setEnabled(!rec.approved());
        approveBtn.addClickListener(e -> openApprovalDialog(rec));

        Button rejectBtn = new Button("Reject Recommendation", VaadinIcon.CLOSE.create());
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        rejectBtn.setEnabled(!rec.approved());
        rejectBtn.addClickListener(e -> openRejectionDialog(rec));

        buttonBar.add(rejectBtn, approveBtn);

        detailsContainer.add(
                title, summary,
                actionsHeader, actionsList,
                evidenceHeader, evidenceGrid,
                constraintsHeader, constraintsGrid,
                tradeoffsHeader, tradeoffsGrid,
                buttonBar
        );
    }

    private void openApprovalDialog(ExplainableRecommendation rec) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Audited Human Authorization Required");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("Autonomous approval is strictly prohibited by governance policy. Enter your authorized manager credentials to sign this recommendation."));

        TextField userField = new TextField("Authorized Manager ID");
        userField.setValue("USR-SCM-01");
        userField.setRequired(true);

        layout.add(userField);
        dialog.add(layout);

        Button confirm = new Button("Sign & Authorize", event -> {
            if (userField.getValue().isBlank()) {
                Notification.show("Manager ID is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (recommendationService != null) {
                recommendationService.approveRecommendation(rec.id(), userField.getValue(), "WS-AUTO-01");
                Notification.show("Recommendation approved by " + userField.getValue(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshData();
            }
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", event -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void openRejectionDialog(ExplainableRecommendation rec) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reject Recovery Recommendation");

        VerticalLayout layout = new VerticalLayout();
        TextField userField = new TextField("Rejecting Manager ID");
        userField.setValue("USR-SCM-01");
        TextArea reasonField = new TextArea("Rejection Justification");
        reasonField.setPlaceholder("State explicit operational reason for rejection...");
        reasonField.setRequired(true);

        layout.add(userField, reasonField);
        dialog.add(layout);

        Button confirm = new Button("Confirm Rejection", event -> {
            if (reasonField.getValue().isBlank()) {
                Notification.show("Rejection reason is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (recommendationService != null) {
                recommendationService.rejectRecommendation(rec.id(), userField.getValue(), "WS-AUTO-01", reasonField.getValue());
                Notification.show("Recommendation rejected", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                refreshData();
            }
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancel = new Button("Cancel", event -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refreshData() {
        if (recommendationService != null) {
            List<ExplainableRecommendation> list = recommendationService.findAll();
            recommendationsGrid.setItems(list);
            if (!list.isEmpty()) {
                displayDetails(list.get(0));
            }
        }
    }
}

