package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.QuotationStatus;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqQuotation;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "collaboration/rfq/compare", layout = MainLayout.class)
@PageTitle("Quotation Comparison & Tender Award | SupplyGraph")
public class QuotationComparisonDashboardView extends CollaborationViewBase {

    private final RfqCollaborationService rfqService;

    public QuotationComparisonDashboardView(RfqCollaborationService rfqService) {
        super("Quotation Comparison & Tender Award Matrix", "Multi-quote evaluation, commercial price vs lead-time Pareto frontier, and governed contract award handoff.");
        this.rfqService = rfqService;

        WorkspaceId ws = currentWorkspaceId();
        List<RfqDocument> rfqs = rfqService.listRfqs(ws);

        HorizontalLayout selectorRow = new HorizontalLayout();
        selectorRow.setAlignItems(Alignment.END);

        ComboBox<RfqDocument> rfqBox = new ComboBox<>("Select RFQ to Evaluate");
        rfqBox.setItems(rfqs);
        rfqBox.setItemLabelGenerator(r -> r.title() + " [" + r.status().name() + " (" + r.quotations().size() + " bids)]");
        rfqBox.setWidth("450px");

        Grid<RfqQuotation> quoteGrid = new Grid<>();

        Button awardBtn = new Button("Award Contract to Selected Quote", e -> {
            RfqDocument currentRfq = rfqBox.getValue();
            RfqQuotation selectedQuote = quoteGrid.asSingleSelect().getValue();
            if (currentRfq == null || selectedQuote == null) {
                Notification.show("Please select an RFQ and a quotation to award", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            RfqDocument awarded = rfqService.awardQuotation(ws, currentRfq.id(), selectedQuote.id());
            Notification.show("Awarded tender " + awarded.id().value() + " to supplier " + selectedQuote.supplierId().value() + "!", 4000, Notification.Position.TOP_CENTER);
            quoteGrid.setItems(awarded.quotations());
        });
        awardBtn.getElement().getThemeList().add("success");

        rfqBox.addValueChangeListener(e -> {
            RfqDocument selected = e.getValue();
            if (selected != null) {
                quoteGrid.setItems(selected.quotations());
            } else {
                quoteGrid.setItems(List.of());
            }
        });

        selectorRow.add(rfqBox, awardBtn);
        add(selectorRow);

        add(new H3("Commercial Quotations Comparison Grid"));
        quoteGrid.addColumn(q -> q.id().value()).setHeader("Quote ID").setAutoWidth(true);
        quoteGrid.addColumn(q -> q.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        quoteGrid.addColumn(q -> "$" + q.unitPriceUsd()).setHeader("Unit Price").setAutoWidth(true);
        quoteGrid.addColumn(q -> q.leadTimeDays() + " days").setHeader("Committed Lead Time").setAutoWidth(true);
        quoteGrid.addColumn(q -> q.minOrderQuantity() + " units").setHeader("MOQ").setAutoWidth(true);
        quoteGrid.addColumn(q -> q.validityDate().toString()).setHeader("Price Validity").setAutoWidth(true);
        quoteGrid.addComponentColumn(q -> {
            Span badge = new Span(q.status().name());
            badge.getElement().getThemeList().add("badge small " + (q.status() == QuotationStatus.ACCEPTED ? "success" : q.status() == QuotationStatus.REJECTED ? "error" : "primary"));
            return badge;
        }).setHeader("Bid Status").setAutoWidth(true);
        quoteGrid.addColumn(q -> q.submittedAt().toString()).setHeader("Submitted At").setAutoWidth(true);

        if (!rfqs.isEmpty()) {
            rfqBox.setValue(rfqs.get(0));
            quoteGrid.setItems(rfqs.get(0).quotations());
        }

        quoteGrid.setHeight("400px");
        add(quoteGrid);
    }
}

