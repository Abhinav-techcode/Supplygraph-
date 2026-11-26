package com.supplygraph.web.views.integration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.connector.ConnectorRegistryService;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Route(value = "connector-config", layout = MainLayout.class)
@PageTitle("Connector Configuration | ORBIX")
public class ConnectorConfigurationView extends IntegrationViewBase {
    private final ConnectorRegistryService service;
    private final ComboBox<Connector> selector = new ComboBox<>("Connector");
    private final TextField baseUri = new TextField("Base URI (https)");
    private final TextField credential = new TextField("Credential reference (env:NAME or vault:path)");
    private final TextField hosts = new TextField("Allowed hosts (comma separated)");
    private final IntegerField timeout = new IntegerField("Timeout (ms)");
    private final IntegerField maxBytes = new IntegerField("Max response bytes");
    private final IntegerField retries = new IntegerField("Max retries");

    public ConnectorConfigurationView(ConnectorRegistryService service) {
        super("Connector Configuration", "Secret-free configuration. Only a credential reference is stored; the secret itself is never saved or displayed.");
        this.service = service;
        selector.setItemLabelGenerator(c -> c.name() + " · " + c.id() + " · " + UiSupport.modeLabel(c.mode()));
        selector.setWidth("32em");
        selector.addValueChangeListener(e -> load(e.getValue()));
        timeout.setValue(5000); maxBytes.setValue(1_048_576); retries.setValue(3);
        timeout.setStepButtonsVisible(true);
        FormLayout form = new FormLayout(baseUri, credential, hosts, timeout, maxBytes, retries);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("40em", 2));
        Button save = new Button("Save configuration", e -> save());
        save.addThemeName("primary");
        body(selector, new Paragraph("Sandbox connectors need no URI or credential; leave them blank."), form, save);
        reload();
    }

    private void reload() { selector.setItems(UiSupport.call(() -> service.list(UiSupport.workspace()), List.<Connector>of())); }

    private void load(Connector c) {
        ConnectorConfiguration cfg = c == null ? null : c.configuration();
        baseUri.setValue(cfg != null && cfg.baseUri() != null ? cfg.baseUri().toString() : "");
        credential.setValue(cfg != null && cfg.credential() != null ? cfg.credential().value() : "");
        hosts.setValue(cfg != null ? String.join(",", cfg.allowedHosts()) : "");
        if (cfg != null) { timeout.setValue(cfg.timeoutMillis()); maxBytes.setValue((int) cfg.maxResponseBytes()); retries.setValue(cfg.maxRetries()); }
    }

    private void save() {
        Connector c = selector.getValue();
        if (c == null) { UiSupport.error(new ValidationException("Select a connector first")); return; }
        try {
            URI uri = baseUri.isEmpty() ? null : URI.create(baseUri.getValue().trim());
            CredentialReference ref = credential.isEmpty() ? null : new CredentialReference(credential.getValue().trim());
            List<String> allowed = hosts.isEmpty() ? List.of() : Arrays.stream(hosts.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            ConnectorConfiguration cfg = new ConnectorConfiguration(uri, ref, allowed, timeout.getValue() == null ? 5000 : timeout.getValue(),
                    maxBytes.getValue() == null ? 1_048_576 : maxBytes.getValue(), retries.getValue() == null ? 3 : retries.getValue());
            if (UiSupport.run("Configuration saved", () -> service.configure(c.id(), cfg))) reload();
        } catch (com.supplygraph.domain.error.ValidationException | IllegalArgumentException e) {
            UiSupport.error(e);
        }
    }
}
