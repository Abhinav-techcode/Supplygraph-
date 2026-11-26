package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.disruption.DisruptionStatus;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Disruption Management View.
 * Provides complete lifecycle management (DRAFT -> VALIDATED -> ACTIVE -> RESOLVED/CANCELLED),
 * incident reporting, and real-time filtering.
 */
@Route(value = "disruptions", layout = MainLayout.class)
@PageTitle("ORBIX :: Disruption Management")
public class DisruptionsView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final Grid<Disruption> grid = new Grid<>(Disruption.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<DisruptionStatus> statusFilter = new ComboBox<>();
    private final ComboBox<DisruptionSeverity> severityFilter = new ComboBox<>();

    public DisruptionsView() {
        this(null);
    }

    @Autowired
    public DisruptionsView(@Autowired(required = false) DisruptionApplicationService disruptionService) {
        this.disruptionService = disruptionService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildFilterBar();
        buildGrid();
        refreshGrid();
    }

    private void buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H2 title = new H2("Disruption Incident Management");
        title.getStyle().set("margin", "0");

        Button btnReport = new Button("Report Disruption", VaadinIcon.PLUS.create(), e -> openReportDialog());
        btnReport.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        header.add(title, btnReport);
        add(header);
    }

    private void buildFilterBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setSpacing(true);

        searchField.setPlaceholder("Search by title or description...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("350px");

        statusFilter.setPlaceholder("Filter by status...");
        statusFilter.setItems(DisruptionStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> refreshGrid());

        severityFilter.setPlaceholder("Filter by severity...");
        severityFilter.setItems(DisruptionSeverity.values());
        severityFilter.setClearButtonVisible(true);
        severityFilter.addValueChangeListener(e -> refreshGrid());

        bar.add(searchField, statusFilter, severityFilter);
        add(bar);
    }

    private void buildGrid() {
        grid.addColumn(Disruption::title).setHeader("Title").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(d -> d.category().name()).setHeader("Category").setAutoWidth(true);

        grid.addComponentColumn(d -> {
            Span badge = new Span(d.severity().name());
            String theme = switch (d.severity()) {
                case CRITICAL -> "badge error";
                case HIGH -> "badge warning";
                default -> "badge";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Severity").setAutoWidth(true);

        grid.addComponentColumn(d -> {
            Span badge = new Span(d.status().name());
            String theme = switch (d.status()) {
                case ACTIVE -> "badge error";
                case VALIDATED -> "badge warning";
                case RESOLVED -> "badge success";
                default -> "badge";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addColumn(d -> d.affectedNodeIds().stream().map(NodeId::value).collect(Collectors.joining(", ")))
                .setHeader("Affected Nodes").setAutoWidth(true);

        grid.addColumn(d -> d.startTime().toString()).setHeader("Reported At").setAutoWidth(true);

        grid.addComponentColumn(this::createActionButtons).setHeader("Lifecycle Actions").setAutoWidth(true);

        add(grid);
    }

    private HorizontalLayout createActionButtons(Disruption d) {
        HorizontalLayout actions = new HorizontalLayout();
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);

        if (d.status() == DisruptionStatus.DRAFT) {
            Button btnValidate = new Button("Validate", VaadinIcon.CHECK.create(), e -> {
                try {
                    disruptionService.validateDisruption(d.id());
                    Notification.show("Disruption validated against digital twin", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Validation failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnValidate.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            actions.add(btnValidate);
        }

        if (d.status() == DisruptionStatus.VALIDATED) {
            Button btnActivate = new Button("Activate", VaadinIcon.BOLT.create(), e -> {
                try {
                    disruptionService.activateDisruption(d.id());
                    Notification.show("Disruption activated! Ready for cascading impact assessment.", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Activation failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnActivate.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            actions.add(btnActivate);
        }

        if (d.status() == DisruptionStatus.ACTIVE) {
            Button btnResolve = new Button("Resolve", VaadinIcon.CHECK_CIRCLE.create(), e -> {
                try {
                    disruptionService.resolveDisruption(d.id());
                    Notification.show("Disruption marked as RESOLVED.", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Resolve failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnResolve.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            actions.add(btnResolve);
        }

        return actions;
    }

    private void openReportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Report Disruption Incident");
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();

        TextField titleField = new TextField("Incident Title");
        titleField.setRequired(true);

        ComboBox<DisruptionCategory> catBox = new ComboBox<>("Disruption Category");
        catBox.setItems(DisruptionCategory.values());
        catBox.setValue(DisruptionCategory.MANUFACTURING_SITE_OUTAGE);

        ComboBox<DisruptionSeverity> sevBox = new ComboBox<>("Severity");
        sevBox.setItems(DisruptionSeverity.values());
        sevBox.setValue(DisruptionSeverity.HIGH);

        TextField nodesField = new TextField("Affected Nodes (comma-separated)");
        nodesField.setValue("FAB-MUNICH");

        TextField matsField = new TextField("Affected Materials (comma-separated)");
        matsField.setValue("MCU-AUTO-32");

        NumberField durationDaysField = new NumberField("Expected Duration (Days)");
        durationDaysField.setValue(14.0);
        durationDaysField.setMin(1.0);

        TextArea descField = new TextArea("Description & Observations");
        descField.setPlaceholder("Detail root cause, affected equipment, or facility condition...");

        form.add(titleField, catBox, sevBox, nodesField, matsField, durationDaysField, descField);
        form.setColspan(descField, 2);

        Button btnSave = new Button("Submit Incident", e -> {
            if (titleField.isEmpty()) {
                Notification.show("Incident Title is required", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
                Set<NodeId> nodeIds = Arrays.stream(nodesField.getValue().split(","))
                        .map(String::trim).filter(s -> !s.isBlank()).map(NodeId::of).collect(Collectors.toSet());
                Set<MaterialId> matIds = Arrays.stream(matsField.getValue().split(","))
                        .map(String::trim).filter(s -> !s.isBlank()).map(MaterialId::of).collect(Collectors.toSet());

                long days = durationDaysField.getValue() != null ? durationDaysField.getValue().longValue() : 14;
                Instant start = Instant.now();
                Instant end = start.plus(Duration.ofDays(days));

                DisruptionImpactParameters params = new DisruptionImpactParameters(
                        BigDecimal.valueOf(100), 86400L * days, (int) days,
                        BigDecimal.valueOf(10000), "PIECES", Map.of()
                );

                if (disruptionService != null) {
                    disruptionService.createDraft(
                            wsId, "AUTOMOTIVE", "OUTAGE",
                            titleField.getValue(), descField.getValue(),
                            catBox.getValue(), sevBox.getValue(),
                            nodeIds, Set.of(), matIds,
                            start, end, params, "Manual operator report"
                    );
                }
                Notification.show("Disruption reported successfully!", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error creating disruption: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(btnCancel, btnSave);
        dialog.open();
    }

    private void refreshGrid() {
        if (disruptionService == null) {
            return;
        }
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        List<Disruption> items = disruptionService.listDisruptions(wsId);

        String search = searchField.getValue();
        DisruptionStatus status = statusFilter.getValue();
        DisruptionSeverity severity = severityFilter.getValue();

        List<Disruption> filtered = items.stream()
                .filter(d -> search == null || search.isBlank()
                        || d.title().toLowerCase().contains(search.toLowerCase())
                        || d.description().toLowerCase().contains(search.toLowerCase()))
                .filter(d -> status == null || d.status() == status)
                .filter(d -> severity == null || d.severity() == severity)
                .toList();

        grid.setItems(filtered);
    }
}

