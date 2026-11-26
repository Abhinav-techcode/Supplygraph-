package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.ApiDeveloperCentreService;
import com.supplygraph.domain.model.platform.ApiKeyRecord;
import com.supplygraph.domain.model.platform.ApiScope;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@PageTitle("API Developer Centre | ORBIX")
@Route(value = "admin/api", layout = MainLayout.class)
public class ApiDeveloperCentreView extends PlatformAdminViewBase {

    private final ApiDeveloperCentreService apiService;
    private final Grid<ApiKeyRecord> grid = new Grid<>();

    public ApiDeveloperCentreView(ApiDeveloperCentreService apiService) {
        super("API Developer Centre", "Manage scoped enterprise API credentials, external webhook authentication, and SDK tokens.");
        this.apiService = apiService;

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setWidthFull();

        Button btnCreate = new Button("Generate New API Key", VaadinIcon.KEY.create(), e -> openGenerateKeyDialog());
        btnCreate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionsBar.add(btnCreate);
        add(actionsBar);

        configureGrid();
        add(grid);
        refreshGrid();

        VerticalLayout docCard = new VerticalLayout();
        docCard.addClassNames("p-m", "border", "rounded-m");
        docCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("margin-top", "var(--lumo-space-l)");

        docCard.add(new H3("ORBIX External Integration API"));
        docCard.add(new Span("Authenticate HTTP requests using the Authorization header with a Bearer token:"));
        Pre curlExample = new Pre(
                "curl -X GET \"https://api.supplygraph.internal/api/v1/twin/nodes\" \\\n" +
                "  -H \"Authorization: Bearer sg_live_YOUR_TOKEN_HERE\" \\\n" +
                "  -H \"X-Workspace-Id: " + currentWorkspaceId().value() + "\" \\\n" +
                "  -H \"Accept: application/json\""
        );
        curlExample.getStyle().set("background", "var(--lumo-contrast-5pct)").set("padding", "var(--lumo-space-m)").set("border-radius", "4px");
        docCard.add(curlExample);
        add(docCard);
    }

    private void configureGrid() {
        grid.addColumn(k -> k.id().value()).setHeader("Key ID").setAutoWidth(true);
        grid.addColumn(ApiKeyRecord::keyName).setHeader("Key Name");
        grid.addColumn(k -> String.join(", ", k.scopes().stream().map(Enum::name).toList())).setHeader("Authorized Scopes");
        grid.addColumn(k -> k.active() ? "ACTIVE" : "REVOKED").setHeader("Status");
        grid.addColumn(ApiKeyRecord::createdAt).setHeader("Created At");

        grid.addComponentColumn(k -> {
            Button btnRevoke = new Button("Revoke", e -> {
                apiService.revokeApiKey(k.id());
                refreshGrid();
                Notification.show("API Key revoked", 2000, Notification.Position.TOP_CENTER);
            });
            btnRevoke.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            btnRevoke.setEnabled(k.active());
            return btnRevoke;
        }).setHeader("Revocation");

        grid.setHeight("350px");
    }

    private void refreshGrid() {
        List<ApiKeyRecord> keys = apiService.listApiKeys(currentWorkspaceId());
        grid.setItems(keys);
    }

    private void openGenerateKeyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Generate Enterprise API Key");

        VerticalLayout layout = new VerticalLayout();
        TextField tfName = new TextField("Key Identifier / Client Description");
        tfName.setPlaceholder("e.g. ERP Ingestion Daemon");
        tfName.setWidthFull();

        CheckboxGroup<ApiScope> cbScopes = new CheckboxGroup<>();
        cbScopes.setLabel("Permitted Scopes");
        cbScopes.setItems(ApiScope.values());
        cbScopes.setValue(java.util.Set.of(ApiScope.READ_TWIN));

        layout.add(tfName, cbScopes);
        dialog.add(layout);

        Button btnGenerate = new Button("Generate Token", e -> {
            if (tfName.getValue() == null || tfName.getValue().isBlank()) {
                Notification.show("Please enter a key name", 2000, Notification.Position.TOP_CENTER);
                return;
            }

            var result = apiService.generateApiKey(currentWorkspaceId(), tfName.getValue(), new ArrayList<>(cbScopes.getValue()));
            dialog.close();
            refreshGrid();

            Dialog keyDialog = new Dialog();
            keyDialog.setHeaderTitle("API Token Generated");
            VerticalLayout keyLayout = new VerticalLayout();
            keyLayout.add(new Span("Copy your API token now. For security purposes, it will never be displayed again:"));
            TextField tfToken = new TextField();
            tfToken.setValue(result.rawApiKey());
            tfToken.setReadOnly(true);
            tfToken.setWidthFull();
            keyLayout.add(tfToken);
            keyDialog.add(keyLayout);

            Button btnCloseKey = new Button("Done", ev -> keyDialog.close());
            btnCloseKey.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            keyDialog.getFooter().add(btnCloseKey);
            keyDialog.open();
        });
        btnGenerate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(btnCancel, btnGenerate);
        dialog.open();
    }
}
