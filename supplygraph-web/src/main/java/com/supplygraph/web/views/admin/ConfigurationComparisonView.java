package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.IndustryPolicyLifecycleService;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Configuration Comparison & Policy Diff | ORBIX")
@Route(value = "admin/industry-policies/diff", layout = MainLayout.class)
public class ConfigurationComparisonView extends PlatformAdminViewBase {

    private final IndustryPolicyLifecycleService policyService;
    private final ComboBox<IndustryPolicyRecord> cbPolicyA = new ComboBox<>("Select Base Revision (A)");
    private final ComboBox<IndustryPolicyRecord> cbPolicyB = new ComboBox<>("Select Target Revision (B)");
    private final TextArea taPolicyA = new TextArea("Revision A JSON");
    private final TextArea taPolicyB = new TextArea("Revision B JSON");
    private final Span diffResultBadge = new Span();

    public ConfigurationComparisonView(IndustryPolicyLifecycleService policyService) {
        super("Configuration Comparison & Policy Diff", "Side-by-side differential comparison of industry configuration policies and risk criteria.");
        this.policyService = policyService;

        List<IndustryPolicyRecord> all = policyService.findAll(currentWorkspaceId());

        cbPolicyA.setItemLabelGenerator(p -> p.industryCode() + " - v" + p.versionNumber() + " (" + p.status() + ")");
        cbPolicyA.setItems(all);
        cbPolicyA.setWidth("350px");

        cbPolicyB.setItemLabelGenerator(p -> p.industryCode() + " - v" + p.versionNumber() + " (" + p.status() + ")");
        cbPolicyB.setItems(all);
        cbPolicyB.setWidth("350px");

        if (all.size() >= 2) {
            cbPolicyA.setValue(all.get(0));
            cbPolicyB.setValue(all.get(1));
        } else if (!all.isEmpty()) {
            cbPolicyA.setValue(all.get(0));
            cbPolicyB.setValue(all.get(0));
        }

        Button btnCompare = new Button("Run Structural Diff", e -> executeDiff());
        btnCompare.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout selectorBar = new HorizontalLayout(cbPolicyA, cbPolicyB, btnCompare);
        selectorBar.setWidthFull();
        selectorBar.setAlignItems(Alignment.BASELINE);
        add(selectorBar);

        diffResultBadge.setVisible(false);
        add(diffResultBadge);

        taPolicyA.setReadOnly(true);
        taPolicyA.setWidthFull();
        taPolicyA.setHeight("350px");

        taPolicyB.setReadOnly(true);
        taPolicyB.setWidthFull();
        taPolicyB.setHeight("350px");

        HorizontalLayout sideBySide = new HorizontalLayout(taPolicyA, taPolicyB);
        sideBySide.setWidthFull();
        add(sideBySide);

        if (cbPolicyA.getValue() != null && cbPolicyB.getValue() != null) {
            executeDiff();
        }
    }

    private void executeDiff() {
        IndustryPolicyRecord a = cbPolicyA.getValue();
        IndustryPolicyRecord b = cbPolicyB.getValue();

        if (a == null || b == null) {
            Notification.show("Please select both revisions to compare", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        var result = policyService.comparePolicies(a.id(), b.id());
        taPolicyA.setValue(result.policyA().policyJson());
        taPolicyB.setValue(result.policyB().policyJson());

        diffResultBadge.setVisible(true);
        if (result.identicalJson()) {
            diffResultBadge.setText("Payload Identical: Both revisions contain matching configuration JSON");
            diffResultBadge.getElement().getThemeList().clear();
            diffResultBadge.getElement().getThemeList().add("badge success");
        } else {
            diffResultBadge.setText("Payload Divergent: Payload length delta of " + result.lengthDifference() + " characters detected");
            diffResultBadge.getElement().getThemeList().clear();
            diffResultBadge.getElement().getThemeList().add("badge warning");
        }
    }
}
