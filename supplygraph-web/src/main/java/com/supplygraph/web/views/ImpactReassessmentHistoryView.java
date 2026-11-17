package com.supplygraph.web.views;

import com.supplygraph.application.service.ContinuousImpactReassessmentService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "reassessments", layout = MainLayout.class)
@PageTitle("Impact Reassessment History | SupplyGraph")
public class ImpactReassessmentHistoryView extends VerticalLayout {

    private final ContinuousImpactReassessmentService reassessmentService;
    private final Grid<ImpactReassessment> grid;
    private final TextField workspaceFilter;

    public ImpactReassessmentHistoryView(ContinuousImpactReassessmentService reassessmentService) {
        this.reassessmentService = reassessmentService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Continuous Impact Reassessment History");
        Paragraph desc = new Paragraph("Subgraphs re-evaluated upon operational changes, highlighting preserved vs invalidated scopes.");

        workspaceFilter = new TextField("Workspace ID", "WS-AUTO-01");
        workspaceFilter.addValueChangeListener(e -> refreshGrid());

        grid = new Grid<>();
        grid.setSizeFull();

        grid.addColumn(r -> r.getId().value()).setHeader("Reassessment ID").setAutoWidth(true);
        grid.addColumn(r -> r.getTriggerEventId() != null ? r.getTriggerEventId().value() : "MANUAL").setHeader("Trigger Event").setAutoWidth(true);
        grid.addColumn(r -> "v" + r.getPreviousStateVersion().value() + " -> v" + r.getNewStateVersion().value()).setHeader("Version Delta").setAutoWidth(true);
        grid.addColumn(r -> r.getAffectedNodeIds().size() + " nodes, " + r.getAffectedLaneIds().size() + " lanes").setHeader("Affected Subgraph").setAutoWidth(true);
        grid.addColumn(r -> r.getAffectedPlanIds().size() + " plans").setHeader("Affected Plans").setAutoWidth(true);
        grid.addColumn(r -> r.getReassessedAt().toString()).setHeader("Reassessed At").setAutoWidth(true);

        grid.addItemClickListener(e -> showDetails(e.getItem()));

        add(title, desc, new HorizontalLayout(workspaceFilter), grid);
        refreshGrid();
    }

    private void refreshGrid() {
        String ws = workspaceFilter.getValue();
        if (ws != null && !ws.isBlank()) {
            grid.setItems(reassessmentService.getReassessments(WorkspaceId.of(ws.trim())));
        } else {
            grid.setItems(List.of());
        }
    }

    private void showDetails(ImpactReassessment reassessment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reassessment Details: " + reassessment.getId().value());

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("Preserved Unaffected Scopes:"));
        UnorderedList preservedList = new UnorderedList();
        for (String p : reassessment.getPreservedScope()) {
            preservedList.add(new ListItem(p));
        }
        layout.add(preservedList);

        layout.add(new Paragraph("Invalidated Scopes:"));
        UnorderedList invalidList = new UnorderedList();
        for (String inv : reassessment.getInvalidatedScope()) {
            invalidList.add(new ListItem(inv));
        }
        layout.add(invalidList);

        dialog.add(layout);
        dialog.open();
    }
}

