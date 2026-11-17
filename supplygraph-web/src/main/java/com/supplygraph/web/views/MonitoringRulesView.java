package com.supplygraph.web.views;

import com.supplygraph.application.service.OperationalMonitoringService;
import com.supplygraph.domain.model.monitoring.MonitoringRule;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "monitoring-rules", layout = MainLayout.class)
@PageTitle("Monitoring Rules | SupplyGraph")
public class MonitoringRulesView extends VerticalLayout {

    private final OperationalMonitoringService monitoringService;
    private final Grid<MonitoringRule> ruleGrid;
    private final ComboBox<String> industryFilter;

    public MonitoringRulesView(OperationalMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Continuous Monitoring Rules & Thresholds");
        Paragraph desc = new Paragraph("Configurable industry monitoring rules evaluated against streaming operational events and digital twin states.");

        industryFilter = new ComboBox<>("Industry Pack");
        industryFilter.setItems("ALL", "AUTOMOTIVE", "EV_BATTERY", "PHARMACEUTICAL", "SEMICONDUCTOR");
        industryFilter.setValue("ALL");
        industryFilter.addValueChangeListener(e -> refreshRules());

        HorizontalLayout filterRow = new HorizontalLayout(industryFilter);
        filterRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        ruleGrid = new Grid<>();
        ruleGrid.setSizeFull();

        ruleGrid.addColumn(r -> r.getId().value()).setHeader("Rule ID").setAutoWidth(true);
        ruleGrid.addColumn(MonitoringRule::getName).setHeader("Rule Name").setAutoWidth(true);
        ruleGrid.addColumn(MonitoringRule::getIndustryApplicability).setHeader("Industry").setAutoWidth(true);
        ruleGrid.addColumn(MonitoringRule::getSeverity).setHeader("Severity").setAutoWidth(true);
        ruleGrid.addColumn(r -> r.getThreshold().toString()).setHeader("Threshold").setAutoWidth(true);
        ruleGrid.addColumn(MonitoringRule::getConditionType).setHeader("Condition Type").setAutoWidth(true);
        ruleGrid.addColumn(MonitoringRule::getTrigger).setHeader("Trigger").setAutoWidth(true);

        ruleGrid.addComponentColumn(rule -> {
            Checkbox cb = new Checkbox(rule.isEnabled());
            cb.addValueChangeListener(ev -> {
                monitoringService.toggleRule(rule.getId(), ev.getValue());
                Notification.show("Rule " + rule.getId().value() + " " + (ev.getValue() ? "Enabled" : "Disabled"), 2000, Notification.Position.BOTTOM_END);
            });
            return cb;
        }).setHeader("Enabled").setAutoWidth(true);

        add(title, desc, filterRow, ruleGrid);
        refreshRules();
    }

    private void refreshRules() {
        List<MonitoringRule> rules = monitoringService.getAllRules();
        String sel = industryFilter.getValue();
        if (sel != null && !"ALL".equalsIgnoreCase(sel)) {
            rules = rules.stream().filter(r -> "ALL".equalsIgnoreCase(r.getIndustryApplicability()) || sel.equalsIgnoreCase(r.getIndustryApplicability())).toList();
        }
        ruleGrid.setItems(rules);
    }
}

