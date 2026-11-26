package com.supplygraph.web.views;

import com.supplygraph.application.intelligence.SupplyChainResilienceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.intelligence.NetworkResilienceAssessment;
import com.supplygraph.domain.model.intelligence.ResilienceFactor;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "resilience-analytics", layout = MainLayout.class)
@PageTitle("Supply Chain Resilience Analytics | ORBIX")
public class SupplyChainResilienceView extends VerticalLayout {

    private final SupplyChainResilienceService resilienceService;
    private final DigitalTwinRepositoryPort digitalTwinRepository;

    private final H1 overallScoreHeading;
    private final Paragraph summaryStats;
    private final Grid<ResilienceFactor> factorGrid;
    private final UnorderedList vulnerabilitiesList;
    private final UnorderedList mitigationsList;

    public SupplyChainResilienceView(
            SupplyChainResilienceService resilienceService,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.resilienceService = resilienceService;
        this.digitalTwinRepository = digitalTwinRepository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Supply Chain Network Resilience & Vulnerability Analytics");
        Paragraph desc = new Paragraph("Deterministic structural evaluation of supply chain network topology, single points of failure (SPOF), Herfindahl-Hirschman supplier concentration, and critical path depth.");

        Button reassessBtn = new Button("Reassess Network Resilience", e -> runAssessment());
        reassessBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Score Card
        VerticalLayout scoreCard = new VerticalLayout();
        scoreCard.setWidth("340px");
        scoreCard.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("background", "var(--lumo-base-color)");

        H4 scoreLabel = new H4("Network Resilience Index");
        scoreLabel.getStyle().set("margin", "0");
        overallScoreHeading = new H1("--/100");
        overallScoreHeading.getStyle().set("margin", "4px 0").set("color", "var(--lumo-primary-color)");
        summaryStats = new Paragraph("SPOF: 0 | HHI: 0 | Critical Path: 0 hops");

        scoreCard.add(scoreLabel, overallScoreHeading, summaryStats);

        HorizontalLayout topRow = new HorizontalLayout(scoreCard, reassessBtn);
        topRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        factorGrid = new Grid<>();
        factorGrid.setHeight("220px");
        factorGrid.addColumn(ResilienceFactor::factorName).setHeader("Resilience Factor").setAutoWidth(true);
        factorGrid.addColumn(f -> String.format("%.1f", f.score())).setHeader("Score (0-100)").setAutoWidth(true);
        factorGrid.addColumn(f -> String.format("%.2f", f.weight())).setHeader("Weight").setWidth("80px");
        factorGrid.addColumn(f -> String.format("%.1f", f.contribution())).setHeader("Contribution").setAutoWidth(true);
        factorGrid.addColumn(ResilienceFactor::explanation).setHeader("Factor Definition & Methodology").setAutoWidth(true);

        vulnerabilitiesList = new UnorderedList();
        mitigationsList = new UnorderedList();

        VerticalLayout vulnsCol = new VerticalLayout(new H4("Identified Network Vulnerabilities"), vulnerabilitiesList);
        vulnsCol.setWidth("50%");

        VerticalLayout mitigationsCol = new VerticalLayout(new H4("Recommended Structural Mitigations"), mitigationsList);
        mitigationsCol.setWidth("50%");

        HorizontalLayout drilldownRow = new HorizontalLayout(vulnsCol, mitigationsCol);
        drilldownRow.setWidthFull();

        add(title, desc, topRow, new H4("Resilience Factor Breakdown"), factorGrid, drilldownRow);

        loadLatest();
    }

    private void loadLatest() {
        Optional<NetworkResilienceAssessment> opt = resilienceService.getLatestAssessment("WS-AUTO-01");
        if (opt.isPresent()) {
            renderAssessment(opt.get());
        } else {
            runAssessment();
        }
    }

    private void runAssessment() {
        Optional<DigitalTwinState> stateOpt = digitalTwinRepository.findByWorkspaceId(WorkspaceId.of("WS-AUTO-01"));
        if (stateOpt.isEmpty()) {
            Notification.show("Digital twin state not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        NetworkResilienceAssessment assessment = resilienceService.assessNetworkResilience("WS-AUTO-01", stateOpt.get());
        renderAssessment(assessment);
        Notification.show("Resilience Assessment Updated: Index " + String.format("%.1f", assessment.overallResilienceIndex()) + "/100", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void renderAssessment(NetworkResilienceAssessment a) {
        overallScoreHeading.setText(String.format("%.1f", a.overallResilienceIndex()) + "/100");
        overallScoreHeading.getStyle().set("color", a.overallResilienceIndex() >= 70.0 ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");

        summaryStats.setText("SPOF: " + a.singlePointsOfFailureCount() +
                " | HHI: " + String.format("%.0f", a.supplierConcentrationHHI()) +
                " | Max Graph Depth: " + a.criticalPathDepth() + " hops" +
                " | Bottlenecks: " + a.recoveryBottlenecksCount());

        factorGrid.setItems(a.factorScores());

        vulnerabilitiesList.removeAll();
        for (String v : a.criticalVulnerabilities()) {
            vulnerabilitiesList.add(new ListItem(v));
        }

        mitigationsList.removeAll();
        for (String m : a.recommendedMitigations()) {
            mitigationsList.add(new ListItem(m));
        }
    }
}

