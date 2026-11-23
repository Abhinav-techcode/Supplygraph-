package com.supplygraph.web.views;

import com.supplygraph.adapter.ai.IncidentInvestigationService;
import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.application.agent.AgentLearningService;
import com.supplygraph.application.agent.AgentTrackRecord;
import com.supplygraph.application.execution.ExecutionOrchestrationService;
import com.supplygraph.application.execution.RecoveryPlanConversionService;
import com.supplygraph.application.graph.NetworkGraphView;
import com.supplygraph.application.graph.NetworkGraphViewService;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.ImpactAnalysisApplicationService;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.disruption.DisruptionStatus;
import com.supplygraph.domain.model.event.EventProvenance;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.impact.ImpactAssessment;
import com.supplygraph.domain.model.impact.ImpactType;
import com.supplygraph.domain.model.impact.MaterialShortageImpact;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.PlanActionType;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.web.components.DigitalTwinNetworkGraphComponent;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Guided incident workspace: the digital twin graph on top, and below it the seven steps of one recovery
 * (detect, impact, agents, options, ConcurGuard, approval, execution). The graph changes with the step: it shows the disrupted
 * facilities, the downstream impact, where agents act, the chosen recovery route and, after execution, the updated stock.
 * Every figure comes from the twin or from a service; where the twin cannot supply a value the page says so.
 */
@Route(value = "incident-workspace", layout = MainLayout.class)
@PageTitle("Guided Incident Workspace | SupplyGraph")
public class IncidentWorkspaceView extends VerticalLayout {

    private static final String[] STEP_LABELS = {"1. Detect", "2. Impact", "3. Agents", "4. Options", "5. ConcurGuard", "6. Approve", "7. Execute"};

    private final DisruptionApplicationService disruptionService;
    private final ImpactAnalysisApplicationService impactService;
    private final IncidentInvestigationService investigationService;
    private final AgentLearningService learningService;
    private final RecoveryPlanningApplicationService planningService;
    private final RecoveryPlanConversionService conversionService;
    private final ExecutionOrchestrationService executionService;
    private final OperationalEventIngestionService eventService;
    private final NetworkGraphViewService graphService;

    private final WorkspaceId ws = WorkspaceId.of("WS-AUTO-01");

    private int currentStep = 1;
    private Disruption selectedDisruption;
    private ImpactAssessment assessment;
    private IncidentInvestigationService.Result investigation;
    private List<RecoveryPlan> candidatePlans = new ArrayList<>();
    private RecoveryPlan selectedPlan;
    private String approver = "";
    private String executionLog = "";
    private String workflowId;
    private final com.supplygraph.application.execution.ExecutionQueryService executionQuery;

    private final HorizontalLayout stepperBar = new HorizontalLayout();
    private final Div graphHolder = new Div();
    private final VerticalLayout stepContent = new VerticalLayout();
    private DigitalTwinNetworkGraphComponent graph;

    @Autowired
    public IncidentWorkspaceView(DisruptionApplicationService disruptionService, ImpactAnalysisApplicationService impactService,
                                 IncidentInvestigationService investigationService, AgentLearningService learningService,
                                 RecoveryPlanningApplicationService planningService, RecoveryPlanConversionService conversionService,
                                 ExecutionOrchestrationService executionService, OperationalEventIngestionService eventService,
                                 NetworkGraphViewService graphService, com.supplygraph.application.execution.ExecutionQueryService executionQuery) {
        this.executionQuery = executionQuery;
        this.disruptionService = disruptionService;
        this.impactService = impactService;
        this.investigationService = investigationService;
        this.learningService = learningService;
        this.planningService = planningService;
        this.conversionService = conversionService;
        this.executionService = executionService;
        this.eventService = eventService;
        this.graphService = graphService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("sg-incident-workspace");

        H2 title = new H2("Guided incident recovery");
        title.getStyle().set("margin-bottom", "0");
        Paragraph subtitle = new Paragraph("Follow one incident from the first signal to a verified recovery. The network map below always shows where you are in the story.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "var(--lumo-space-xs)");

        stepperBar.setWidthFull();
        stepperBar.addClassName("sg-stepper");
        stepperBar.getStyle().set("flex-wrap", "wrap").set("background", "var(--lumo-contrast-5pct)").set("border-radius", "var(--lumo-border-radius-m)").set("padding", "var(--lumo-space-s)");
        graphHolder.setWidthFull();
        graphHolder.addClassName("sg-graph-holder");
        stepContent.setPadding(false);
        stepContent.setWidthFull();
        stepContent.addClassName("sg-step-content");

        add(title, subtitle, stepperBar, graphHolder, stepContent);

        loadInitialDisruption();
        rebuildGraph();
        refreshStepper();
        renderStep();
    }

    // ---------- shell ----------

    private void loadInitialDisruption() {
        List<Disruption> list = disruptionService.listDisruptions(ws);
        selectedDisruption = list.stream().filter(d -> d.status() == DisruptionStatus.ACTIVE).findFirst().or(() -> list.stream().findFirst()).orElse(null);
    }

    private void refreshStepper() {
        stepperBar.removeAll();
        for (int i = 0; i < STEP_LABELS.length; i++) {
            int n = i + 1;
            Button b = new Button(STEP_LABELS[i]);
            b.addClassName("sg-step-" + n);
            b.addThemeVariants(n == currentStep ? ButtonVariant.LUMO_PRIMARY : n < currentStep ? ButtonVariant.LUMO_SUCCESS : ButtonVariant.LUMO_TERTIARY);
            b.addClickListener(e -> go(n));
            stepperBar.add(b);
        }
    }

    private void go(int step) {
        currentStep = Math.max(1, Math.min(7, step));
        refreshStepper();
        renderStep();
        updateOverlay();
    }

    private void rebuildGraph() {
        graphHolder.removeAll();
        graph = new DigitalTwinNetworkGraphComponent(graphService.build(ws));
        graphHolder.add(graph);
        updateOverlay();
    }

    private HorizontalLayout navigation(boolean nextEnabled, String hint) {
        Button prev = new Button("Previous", VaadinIcon.ARROW_LEFT.create(), e -> go(currentStep - 1));
        prev.setEnabled(currentStep > 1);
        Button next = new Button("Next", VaadinIcon.ARROW_RIGHT.create(), e -> go(currentStep + 1));
        next.setIconAfterText(true);
        next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        next.addClassName("sg-next-step");
        next.setEnabled(nextEnabled && currentStep < 7);
        HorizontalLayout bar = new HorizontalLayout(prev);
        if (hint != null && !hint.isBlank()) {
            Span h = new Span(hint);
            h.getStyle().set("color", "var(--lumo-secondary-text-color)").set("align-self", "center");
            bar.add(h);
        }
        bar.add(next);
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return bar;
    }

    private void renderStep() {
        stepContent.removeAll();
        switch (currentStep) {
            case 1 -> step1();
            case 2 -> step2();
            case 3 -> step3();
            case 4 -> step4();
            case 5 -> step5();
            case 6 -> step6();
            case 7 -> step7();
        }
    }

    private void updateOverlay() {
        if (graph == null) return;
        Set<String> impacted = new LinkedHashSet<>();
        Set<String> marked = new LinkedHashSet<>();
        List<String> route = new ArrayList<>();
        if (currentStep >= 2 && assessment != null) {
            assessment.nodeImpacts().forEach(ni -> impacted.add(ni.nodeId().value()));
        }
        if (currentStep == 3 && investigation != null) {
            investigation.proposal().approvedIntents().forEach(i -> {
                marked.add(i.targetEntityId());
                if (i.location() != null) marked.add(i.location());
            });
        }
        if (currentStep >= 4 && selectedPlan != null) {
            for (PlanAction a : selectedPlan.actions()) {
                String r = a.parameters() == null ? null : a.parameters().get("route");
                if (r != null) route.addAll(Arrays.stream(r.split(">")).map(String::trim).toList());
                if (a.targetNodeId() != null) marked.add(a.targetNodeId().value());
            }
        }
        graph.setOverlay(new DigitalTwinNetworkGraphComponent.Overlay(impacted, route, marked));
    }

    private static Div card() {
        Div c = new Div();
        c.getStyle().set("background", "var(--lumo-base-color)").set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "8px").set("padding", "16px").set("width", "100%").set("box-sizing", "border-box");
        return c;
    }

    private static Span badge(String text, String theme) {
        Span s = new Span(text);
        s.getElement().getThemeList().add(theme);
        return s;
    }

    private static Div kpi(String label, String value, String note) {
        Div d = card();
        d.getStyle().set("flex", "1 1 180px").set("width", "auto");
        Span v = new Span(value);
        v.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "700").set("display", "block");
        Span l = new Span(label);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)").set("display", "block");
        d.add(v, l);
        if (note != null) {
            Span n = new Span(note);
            n.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-tertiary-text-color)").set("display", "block");
            d.add(n);
        }
        return d;
    }

    private static Div kpiRow(Div... items) {
        Div row = new Div(items);
        row.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px").set("width", "100%");
        return row;
    }

    private static UnorderedList bullets(Collection<String> items) {
        UnorderedList ul = new UnorderedList();
        items.forEach(i -> ul.add(new ListItem(i)));
        return ul;
    }

    private void needIncident(H3 header) {
        stepContent.add(header, new Paragraph("Choose an incident in step 1 first."), navigation(false, null));
    }

    // ---------- step 1: detect ----------

    private void step1() {
        H3 header = new H3("Step 1 · Detect: what happened?");
        Paragraph help = new Paragraph("Pick the incident to work on. Disrupted facilities are drawn in red on the map above.");

        ComboBox<Disruption> selector = new ComboBox<>("Incident");
        selector.addClassName("sg-incident-select");
        selector.setWidth("480px");
        List<Disruption> list = disruptionService.listDisruptions(ws);
        selector.setItems(list);
        selector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        if (selectedDisruption != null) selector.setValue(selectedDisruption);

        Div details = card();
        Runnable fill = () -> {
            details.removeAll();
            if (selectedDisruption == null) {
                details.add(new Paragraph("No incident is registered for this workspace. Register one, or load the sample network from the home page."));
                return;
            }
            Disruption d = selectedDisruption;
            details.add(new H4(d.title()), badge(d.severity().name(), d.severity() == DisruptionSeverity.CRITICAL ? "badge error" : "badge warning"), new Span(" "), badge(d.category().name().replace('_', ' '), "badge contrast"));
            details.add(new Paragraph("Affected facilities: " + (d.affectedNodeIds().isEmpty() ? "none declared" : d.affectedNodeIds().stream().map(NodeId::value).sorted().reduce((a, b) -> a + ", " + b).orElse(""))));
            if (d.expectedEndTime() != null) details.add(new Paragraph("Expected duration: " + Duration.between(d.startTime(), d.expectedEndTime()).toDays() + " days"));
            else details.add(new Paragraph("Expected duration: not declared (72 hours are assumed for estimates)."));
            if (d.description() != null && !d.description().isBlank()) details.add(new Paragraph(d.description()));
        };
        fill.run();
        selector.addValueChangeListener(e -> {
            selectedDisruption = e.getValue();
            assessment = null; investigation = null; candidatePlans = new ArrayList<>(); selectedPlan = null; executionLog = ""; workflowId = null;
            fill.run();
            updateOverlay();
        });

        NetworkGraphView view = graphService.build(ws);
        boolean hasPort = view.nodes().stream().anyMatch(n -> n.id().equals("PORT-ROTTERDAM"));
        Button example = new Button("Add example incident: Rotterdam port closure", VaadinIcon.PLUS_CIRCLE.create(), e -> {
            Disruption d = disruptionService.createDisruption(ws, "AUTOMOTIVE", "PORT_STRIKE", "Port of Rotterdam dockworker strike",
                    "Berth closure halts container transshipment for two weeks.", DisruptionCategory.LOGISTICS_HUB_CLOSURE, DisruptionSeverity.CRITICAL,
                    Set.of(NodeId.of("PORT-ROTTERDAM")), Set.of(), Set.of(), Instant.now(), Instant.now().plus(Duration.ofDays(14)), DisruptionImpactParameters.empty(), "MANUAL_EXAMPLE");
            selectedDisruption = disruptionService.activateDisruption(d.id());
            Notification.show("Incident registered and activated.", 2500, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            rebuildGraph();
            renderStep();
        });
        example.setEnabled(hasPort);
        if (!hasPort) example.getElement().setAttribute("title", "This workspace's network has no PORT-ROTTERDAM facility.");

        HorizontalLayout bar = new HorizontalLayout(selector, example);
        bar.setAlignItems(FlexComponent.Alignment.BASELINE);
        bar.getStyle().set("flex-wrap", "wrap");
        stepContent.add(header, help, bar, details, navigation(selectedDisruption != null, "Next: see what this incident reaches."));
    }

    // ---------- step 2: impact ----------

    private void ensureAssessment() {
        if (assessment == null && selectedDisruption != null) assessment = impactService.analyzeCascadingImpact(selectedDisruption.id(), ws);
        updateOverlay();
    }

    private void step2() {
        H3 header = new H3("Step 2 · Impact: what does it reach?");
        if (selectedDisruption == null) { needIncident(header); return; }
        try {
            ensureAssessment();
        } catch (RuntimeException ex) {
            stepContent.add(header, new Paragraph("The impact could not be computed: " + ex.getMessage()), navigation(false, null));
            return;
        }
        long direct = assessment.nodeImpacts().stream().filter(n -> n.impactType() == ImpactType.DIRECT).count();
        long indirect = assessment.nodeImpacts().stream().filter(n -> n.impactType() == ImpactType.INDIRECT).count();
        BigDecimal shortage = assessment.materialShortages().stream().map(MaterialShortageImpact::shortageQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        OptionalDouble survive = assessment.materialShortages().stream().filter(m -> m.stockoutEstimatedTime() != null)
                .mapToDouble(m -> Duration.between(Instant.now(), m.stockoutEstimatedTime()).toMinutes() / 1440.0).min();
        long spof = assessment.riskIndicators().stream().filter(r -> r.startsWith("Single source") || r.startsWith("Facility ")).count();

        Div kpis = kpiRow(
                kpi("Facilities hit directly", String.valueOf(direct), "disrupted in the twin"),
                kpi("Facilities reached downstream", String.valueOf(indirect), "orange on the map"),
                kpi("Shortage over the outage", assessment.materialShortages().isEmpty() || assessment.materialShortages().stream().allMatch(m -> m.requiredQuantity().signum() == 0) ? "No data" : String.format("%,.0f units", shortage), "requirement minus reachable stock"),
                kpi("Time to survive", survive.isPresent() ? String.format("%.1f days", Math.max(0, survive.getAsDouble())) : "No data", "exposed site's stock / daily throughput"),
                kpi("Single points of failure", String.valueOf(spof), "no way around the disruption"),
                kpi("Financial exposure", "Not estimated", "the twin holds no prices"));

        Div why = card();
        why.add(new H4("What the analysis found"));
        if (assessment.riskIndicators().isEmpty()) why.add(new Paragraph("Nothing downstream is reached by this incident."));
        else why.add(bullets(assessment.riskIndicators()));
        Details basis = new Details("How this was computed (assumptions)", bullets(assessment.assumptions()));
        Details missing = new Details("What the twin could not tell us (" + assessment.unknownOrMissingData().size() + ")", bullets(assessment.unknownOrMissingData()));
        missing.setOpened(!assessment.unknownOrMissingData().isEmpty());

        Grid<com.supplygraph.domain.model.impact.NodeImpact> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.addColumn(n -> n.nodeId().value()).setHeader("Facility").setAutoWidth(true);
        grid.addColumn(n -> n.nodeType().name().replace('_', ' ')).setHeader("Type").setAutoWidth(true);
        grid.addColumn(n -> n.impactType() == ImpactType.DIRECT ? "Disrupted" : "Downstream").setHeader("Effect").setAutoWidth(true);
        grid.addColumn(n -> n.impactType() == ImpactType.DIRECT ? n.capacityLossPercentage() + "% capacity lost" : "Supply interrupted upstream").setHeader("Detail").setAutoWidth(true);
        grid.setItems(assessment.nodeImpacts());
        stepContent.add(header, kpis, why, grid, basis, missing, navigation(true, "Next: see what the eight specialist agents propose."));
    }

    // ---------- step 3: agents ----------

    private void step3() {
        H3 header = new H3("Step 3 · Agents: who proposes what?");
        if (selectedDisruption == null) { needIncident(header); return; }
        if (investigation == null) {
            try {
                investigation = investigationService.investigate(ws, selectedDisruption);
            } catch (RuntimeException ex) {
                stepContent.add(header, new Paragraph("The agents could not run: " + ex.getMessage()), navigation(false, null));
                return;
            }
            updateOverlay();
        }
        Paragraph intro = new Paragraph("Eight specialist agents read the twin through a bounded, audited toolbox and each propose what they can see. "
                + "The figure shown is data completeness: how many of the facts each agent needs were found in the twin. It is not a probability. "
                + "The agents are rule-based Java analysis (no language model). They only propose: ConcurGuard arbitrates, and a person approves.");
        Map<String, AgentTrackRecord> track = learningService.trackRecords(ws);

        Grid<AgentInvestigationRecord> grid = new Grid<>();
        grid.addClassName("sg-agent-grid");
        grid.setAllRowsVisible(true);
        grid.addColumn(AgentInvestigationRecord::agentName).setHeader("Agent").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::targetEntity).setHeader("Acts on").setAutoWidth(true);
        grid.addColumn(r -> r.intentsGenerated() == 0 ? "Advisory" : r.intentsGenerated() + " action").setHeader("Proposal").setAutoWidth(true);
        grid.addColumn(r -> Math.round(r.confidenceScore() * 100) + "% of needed facts found").setHeader("Data completeness").setAutoWidth(true);
        grid.addColumn(r -> track.getOrDefault(r.role(), AgentTrackRecord.empty(r.role())).summary()).setHeader("Track record").setAutoWidth(true);
        grid.addComponentColumn(r -> badge(switch (r.outcome()) {
            case "ARBITRATED_ALLOW" -> "Accepted by ConcurGuard";
            case "ADVISORY" -> "Advice only";
            case "REJECTED_BY_CONCURGUARD" -> "Held by ConcurGuard (needs a human)";
            case "REPLACED_BY_REPLANNING" -> "Replanned";
            default -> r.outcome();
        }, "ARBITRATED_ALLOW".equals(r.outcome()) ? "badge success small" : "REJECTED_BY_CONCURGUARD".equals(r.outcome()) ? "badge error small" : "badge contrast small")).setHeader("ConcurGuard").setAutoWidth(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(r -> {
            Div d = new Div();
            d.getStyle().set("padding", "8px 16px");
            d.add(new Span("Evidence from the twin (" + r.toolsInvoked() + " tool call(s)):"));
            d.add(bullets(Arrays.stream(r.summary().split(" \\| ")).filter(x -> !x.isBlank()).toList()));
            return d;
        }));
        grid.setItems(investigation.records());
        grid.setDetailsVisibleOnClick(true);

        stepContent.add(header, intro, grid);
        if (!investigation.proposal().arbitratedTradeOffs().isEmpty()) {
            Div t = card();
            t.add(new H4("ConcurGuard arbitration notes"), bullets(investigation.proposal().arbitratedTradeOffs()));
            stepContent.add(t);
        }
        Paragraph tip = new Paragraph("Tip: click an agent row to read the evidence behind its proposal. Purple dashed boxes on the map show where the accepted actions apply.");
        tip.getStyle().set("color", "var(--lumo-secondary-text-color)");
        stepContent.add(tip, navigation(true, "Next: compare complete recovery options."));
    }

    // ---------- step 4: options ----------

    private static boolean derived(RecoveryPlan p) {
        return p.actions().stream().anyMatch(a -> a.parameters() != null && a.parameters().containsKey("estimate"));
    }

    private void loadPlans() {
        long twinVersion = graphService.build(ws).twinVersion();
        // Only plans made against the current twin are usable: a plan from an older twin version would be refused as stale.
        candidatePlans = new ArrayList<>(planningService.listPlansByDisruption(selectedDisruption.id()).stream()
                .filter(p -> derived(p) && p.status() != RecoveryPlanStatus.SUPERSEDED && p.status() != RecoveryPlanStatus.REJECTED
                        && p.sourceTwinVersion().value() == twinVersion).toList());
        if (candidatePlans.isEmpty()) candidatePlans = new ArrayList<>(planningService.generateAlternativePlans(selectedDisruption.id(), ws));
    }

    private void step4() {
        H3 header = new H3("Step 4 · Options: how can we recover?");
        if (selectedDisruption == null) { needIncident(header); return; }
        try {
            ensureAssessment();
            loadPlans();
        } catch (RuntimeException ex) {
            stepContent.add(header, new Paragraph("No recovery option can be derived: " + ex.getMessage()), navigation(false, null));
            return;
        }
        Paragraph intro = new Paragraph("Each option is derived from the twin: stock that is actually on hand, suppliers that can still reach the plant over undisrupted lanes, and the lane costs along the route. "
                + "Costs are transport costs only (the twin holds no item prices).");
        Div cards = new Div();
        cards.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px").set("width", "100%");
        for (RecoveryPlan p : candidatePlans) {
            Div c = card();
            boolean sel = selectedPlan != null && selectedPlan.id().equals(p.id());
            c.getStyle().set("flex", "1 1 280px").set("width", "auto").set("border", sel ? "2px solid var(--lumo-primary-color)" : "1px solid var(--lumo-contrast-20pct)");
            var m = p.metrics();
            c.add(new H4(p.planName()), new Paragraph(p.description()));
            Div facts = new Div();
            facts.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr").set("gap", "4px 12px").set("font-size", "var(--lumo-font-size-s)");
            facts.add(new Span("Transport cost"), new Span("$" + m.totalCost().toPlainString()));
            facts.add(new Span("Time to deliver"), new Span(String.format("%.1f days", m.recoveryDurationDays())));
            facts.add(new Span("Covers requirement"), new Span(String.format("%.0f%%", m.serviceLevelImpactPercentage())));
            facts.add(new Span("Still short"), new Span(m.unmetDemandQuantity().toPlainString() + " units"));
            facts.add(new Span("Stock used"), new Span(m.inventoryConsumedQuantity().toPlainString() + " units"));
            c.add(facts);
            Button pick = new Button(sel ? "Selected" : "Choose this option", e -> {
                selectedPlan = p;
                updateOverlay();
                renderStep();
            });
            pick.addClassName("sg-choose-plan");
            pick.getStyle().set("margin-top", "var(--lumo-space-s)");
            if (sel) pick.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            c.add(pick);
            cards.add(c);
        }
        Paragraph tip = new Paragraph("The green animated route on the map is the chosen option's path.");
        tip.getStyle().set("color", "var(--lumo-secondary-text-color)");
        stepContent.add(header, intro, cards, tip, navigation(selectedPlan != null, selectedPlan == null ? "Choose an option to continue." : "Next: validate it with ConcurGuard."));
    }

    // ---------- step 5: ConcurGuard ----------

    private void step5() {
        H3 header = new H3("Step 5 · ConcurGuard: is it safe?");
        if (selectedPlan == null) {
            stepContent.add(header, new Paragraph("Choose a recovery option in step 4 first."), navigation(false, null));
            return;
        }
        Div claims = card();
        claims.add(new H4("What this plan claims from the network"));
        for (PlanAction a : selectedPlan.actions()) {
            claims.add(new Paragraph("• " + a.description()));
        }
        Div result = card();
        Button validate = new Button("Run ConcurGuard validation", VaadinIcon.SHIELD.create(), e -> {
            try {
                selectedPlan = planningService.validatePlanWithConcurGuard(selectedPlan.id());
                renderStep();
            } catch (RuntimeException ex) {
                Notification.show("Validation failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        validate.addClassName("sg-validate");
        validate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boolean validated = selectedPlan.status() == RecoveryPlanStatus.VALIDATED || selectedPlan.status() == RecoveryPlanStatus.PENDING_APPROVAL
                || selectedPlan.status() == RecoveryPlanStatus.APPROVED || selectedPlan.concurGuardValidated() || !selectedPlan.concurGuardConflictIds().isEmpty();
        if (!validated) {
            result.add(new Paragraph("ConcurGuard checks the plan's resource claims against the twin's current state and against other plans: overdrafts, double bookings, stale state."), validate);
        } else if (selectedPlan.concurGuardConflictIds().isEmpty()) {
            result.add(badge("No conflicts found", "badge success"), new Paragraph("The plan's claims are consistent with the twin (version " + selectedPlan.sourceTwinVersion().value() + ") and with the other plans."));
        } else {
            result.add(badge(selectedPlan.concurGuardConflictIds().size() + " conflict(s) found", "badge error"), bullets(selectedPlan.concurGuardConflictIds()),
                    new Paragraph("Go back to step 4 and choose a different option."));
        }
        boolean ok = validated && selectedPlan.concurGuardConflictIds().isEmpty();
        stepContent.add(header, claims, result, navigation(ok, ok ? "Next: a person decides." : "Validate the plan to continue."));
    }

    // ---------- step 6: approval ----------

    private void step6() {
        H3 header = new H3("Step 6 · Approve: a person decides");
        if (selectedPlan == null) {
            stepContent.add(header, new Paragraph("Choose and validate a recovery option first."), navigation(false, null));
            return;
        }
        if (selectedPlan.status() == RecoveryPlanStatus.APPROVED) {
            Div done = card();
            done.add(badge("Approved", "badge success"), new Paragraph("Approved by " + selectedPlan.approval().approvedBy() + ". The agents that proposed actions for this incident now have this decision in their track record."));
            stepContent.add(header, done, navigation(true, "Next: execute it."));
            return;
        }
        if (selectedPlan.status() != RecoveryPlanStatus.PENDING_APPROVAL) {
            stepContent.add(header, new Paragraph("The plan is " + selectedPlan.status() + "; validate it with ConcurGuard in step 5 first."), navigation(false, null));
            return;
        }
        Div form = card();
        TextField who = new TextField("Approving as (your signed-in account)");
        who.setWidth("320px");
        var me = com.supplygraph.application.security.SecurityContext.getCurrentUser();
        if (me != null) { who.setValue(me.id().value()); who.setReadOnly(true); }   // the approval must be traceable to a real user, so it is never typed in
        else who.setValue(approver);
        TextArea why = new TextArea("Reason for the decision");
        why.setWidthFull();
        why.setMinHeight("90px");
        why.setRequiredIndicatorVisible(true);
        Button approve = new Button("Approve plan", VaadinIcon.CHECK_CIRCLE.create(), e -> decide(true, who.getValue(), why.getValue()));
        approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        approve.addClassName("sg-approve");
        Button reject = new Button("Reject plan", VaadinIcon.CLOSE_CIRCLE.create(), e -> decide(false, who.getValue(), why.getValue()));
        reject.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        form.add(who, why, new HorizontalLayout(approve, reject));
        stepContent.add(header, new Paragraph("Approving records your name and reason in the audit trail and counts as evidence that the agents' proposals were sound. Rejecting every option counts against them."), form,
                navigation(false, "Decide to continue."));
    }

    private void decide(boolean approve, String who, String reason) {
        if (who == null || who.isBlank() || reason == null || reason.isBlank()) {
            Notification.show("Enter the approver and the reason.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            approver = who.trim();
            if (approve) {
                selectedPlan = planningService.approvePlan(selectedPlan.id(), approver, reason.trim());
                Notification.show("Plan approved.", 2500, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                go(7);
            } else {
                selectedPlan = planningService.rejectPlan(selectedPlan.id(), approver, reason.trim());
                selectedPlan = null;
                candidatePlans = new ArrayList<>();
                Notification.show("Plan rejected. Choose another option.", 3000, Notification.Position.TOP_CENTER);
                go(4);
            }
        } catch (RuntimeException ex) {
            if (ex instanceof com.supplygraph.domain.error.ConflictException) {
                selectedPlan = null;
                candidatePlans = new ArrayList<>();
                Notification.show("The network changed after this plan was made, so it must be re-derived. Choose an option again.", 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                go(4);
                return;
            }
            Notification.show("Decision failed: " + com.supplygraph.web.error.UiErrors.describe(ex, org.slf4j.LoggerFactory.getLogger(IncidentWorkspaceView.class)), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ---------- step 7: execution ----------

    private Optional<PlanAction> transfer() {
        return selectedPlan.actions().stream().filter(a -> a.actionType() == PlanActionType.EMERGENCY_BUFFER_RELEASE && a.parameters() != null
                && a.parameters().containsKey("sourceNode") && a.targetMaterialId() != null && a.targetNodeId() != null).findFirst();
    }

    private double onHand(NetworkGraphView v, String node) {
        return v.nodes().stream().filter(n -> n.id().equals(node)).mapToDouble(NetworkGraphView.Node::onHandUnits).findFirst().orElse(0);
    }

    private void step7() {
        H3 header = new H3("Step 7 · Execute: make it happen, then see the twin change");
        if (selectedPlan == null && selectedDisruption != null) {   // coming back after signing in again: pick up the most recently approved plan of this incident
            planningService.listPlansByDisruption(selectedDisruption.id()).stream().filter(p -> p.status() == RecoveryPlanStatus.APPROVED)
                    .max(Comparator.comparing(RecoveryPlan::updatedAt)).ifPresent(p -> selectedPlan = p);
        }
        if (selectedPlan == null || selectedPlan.status() != RecoveryPlanStatus.APPROVED) {
            stepContent.add(header, new Paragraph("The plan must be approved in step 6 before it can be executed."), navigation(false, null));
            return;
        }
        Div card = card();
        card.add(new H4(selectedPlan.planName()));
        Optional<PlanAction> tr = transfer();
        Div log = new Div();
        log.getStyle().set("white-space", "pre-wrap").set("font-family", "monospace").set("font-size", "var(--lumo-font-size-s)");
        log.setText(executionLog);

        Div approvals = new Div();
        approvals.addClassName("sg-approvals");
        Button start = new Button("2. Start execution", VaadinIcon.ROCKET.create());
        start.addClassName("sg-start-exec");
        Runnable refreshApprovals = () -> {
            approvals.removeAll();
            start.setEnabled(false);
            if (workflowId == null && selectedPlan != null) {   // pick up a workflow made earlier (for example before you signed out to collect approvals)
                executionQuery.list(ws, 100).stream().filter(x -> x.planId().equals(selectedPlan.id().value())).findFirst().ifPresent(x -> workflowId = x.id());
            }
            if (workflowId == null) return;
            var w = executionQuery.get(workflowId);
            Span st = new Span("Workflow " + w.status());
            st.getElement().getThemeList().add(w.status() == com.supplygraph.domain.model.execution.WorkflowStatus.READY || w.status() == com.supplygraph.domain.model.execution.WorkflowStatus.COMPLETED ? "badge success" : "badge contrast");
            approvals.add(st);
            for (var a : w.approvals()) approvals.add(new Paragraph("Approved (" + a.scope() + ") by " + a.userId() + " as " + a.role() + ": " + a.rationale()));
            boolean needWorkflow = !w.hasApproval(com.supplygraph.domain.model.execution.WorkflowApproval.Scope.WORKFLOW);
            boolean needSensitive = w.hasSensitiveTasks() && !w.hasApproval(com.supplygraph.domain.model.execution.WorkflowApproval.Scope.SENSITIVE_TASKS);
            if (w.status() == com.supplygraph.domain.model.execution.WorkflowStatus.BLOCKED) {
                String why = executionQuery.timeline(workflowId).stream().filter(ev -> "WORKFLOW_BLOCKED".equals(ev.type())).map(ev -> ev.detail()).reduce((x, y) -> y).orElse("no reason recorded");
                approvals.add(new Paragraph("This workflow is blocked and cannot be approved or started: " + why + ". Re-derive and approve the plan again (step 4 onwards) to create a new workflow."));
            } else if (w.status() == com.supplygraph.domain.model.execution.WorkflowStatus.PENDING_APPROVAL && (needWorkflow || needSensitive)) {
                approvals.add(new Paragraph("Waiting for " + ((needWorkflow ? 1 : 0) + (needSensitive ? 1 : 0)) + " approval(s) from people other than the creator (" + w.createdBy() + "), and the two approvals must come from two different people: "
                        + (needWorkflow ? "the workflow approval" : "") + (needWorkflow && needSensitive ? " and " : "") + (needSensitive ? "the sensitive-task approval" : "") + ". "
                        + "Only roles with approval rights can do this (supply chain manager or admin). Sign out, sign in as a different such account (demo: admin, same password as manager; a sensitive-task approval needs a third approver), open Execution > Approvals, approve, then sign back in here, reopen this step and press \"Refresh status\"."));
            }
            start.setEnabled(w.status() == com.supplygraph.domain.model.execution.WorkflowStatus.READY);
        };
        Button dispatch = new Button("1. Create governed workflow", VaadinIcon.TASKS.create(), e -> {
            try {
                ExecutionWorkflow wf = conversionService.convert(selectedPlan.id().value());
                workflowId = wf.id();
                executionLog += "Workflow " + wf.id() + " created from the approved plan. It needs separation-of-duties approvals before it can run.\n";
            } catch (RuntimeException ex) {
                executionLog += "Workflow could not be created: " + com.supplygraph.web.error.UiErrors.describe(ex, org.slf4j.LoggerFactory.getLogger(IncidentWorkspaceView.class)) + "\n";
            }
            log.setText(executionLog);
            refreshApprovals.run();
        });
        dispatch.addClassName("sg-dispatch");
        Button refresh = new Button("Refresh status", VaadinIcon.REFRESH.create(), e -> refreshApprovals.run());
        start.addClickListener(e -> {
            try {
                executionService.start(workflowId);
                executionLog += "Workflow " + workflowId + " started. Tasks run on the SIMULATED sandbox connector: nothing was sent to a real system.\n";
            } catch (RuntimeException ex) {
                executionLog += "Workflow could not be started: " + com.supplygraph.web.error.UiErrors.describe(ex, org.slf4j.LoggerFactory.getLogger(IncidentWorkspaceView.class)) + "\n";
            }
            log.setText(executionLog);
            refreshApprovals.run();
        });
        refreshApprovals.run();

        Button confirm = new Button("3. Confirm the stock was moved (operator evidence)", VaadinIcon.CHECK.create(), e -> {
            PlanAction a = tr.orElseThrow();
            NetworkGraphView before = graphService.build(ws);
            String src = a.parameters().get("sourceNode"), dst = a.targetNodeId().value();
            Map<String, String> payload = new HashMap<>();
            payload.put("taskType", "BUFFER_TRANSFER");
            payload.put("quantity", a.quantity().toPlainString());
            payload.put("materialId", a.targetMaterialId().value());
            payload.put("sourceNodeId", src);
            payload.put("targetNodeId", dst);
            payload.put("planId", selectedPlan.id().value());
            OperationalEvent evt = OperationalEvent.createIngested(EventType.RECOVERY_ACTION_COMPLETED, EventSource.MANUAL_OPERATOR, Instant.now(), dst, "FACILITY", ws, payload,
                    new EventProvenance("OPERATOR_CONFIRMATION", approver.isBlank() ? "operator" : approver, "PLAN-" + selectedPlan.id().value(), "MANUAL", Instant.now()),
                    "exec-" + selectedPlan.id().value() + "-" + a.actionId());
            eventService.ingestEvent(evt);
            NetworkGraphView after = graphService.build(ws);
            if (after.twinVersion() == before.twinVersion()) {
                executionLog += "The twin did NOT change: the stock movement was refused (for example the source facility holds less than the quantity). Nothing was moved.\n";
                log.setText(executionLog);
                Notification.show("The twin was not changed: the movement was refused.", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            executionLog += "Operator evidence recorded (manual confirmation, not machine-verified).\n"
                    + "Twin v" + before.twinVersion() + " -> v" + after.twinVersion() + "\n"
                    + src + ": " + String.format("%,.0f", onHand(before, src)) + " -> " + String.format("%,.0f", onHand(after, src)) + " units on hand\n"
                    + dst + ": " + String.format("%,.0f", onHand(before, dst)) + " -> " + String.format("%,.0f", onHand(after, dst)) + " units on hand\n";
            log.setText(executionLog);
            rebuildGraph();
            Notification.show("Twin updated to v" + after.twinVersion() + ".", 3500, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirm.addClassName("sg-confirm-exec");
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirm.setEnabled(tr.isPresent());

        card.add(new Paragraph("Step 1 hands the plan to the governed workflow engine, which then needs approvals from other people (separation of duties). Step 2 runs it once those are in. "
                + "Step 3 records that the physical transfer happened; the twin then moves the stock in its ledger and the map above updates."));
        if (tr.isEmpty()) card.add(new Paragraph("This option changes orders or schedules rather than moving stock, so there is no inventory movement to reconcile into the twin."));
        card.add(new HorizontalLayout(dispatch, refresh, start, confirm), approvals, new H5("Trace"), log);
        stepContent.add(header, card, navigation(false, "This is the last step."));
    }
}
