package com.supplygraph.web.views.admin;

import com.supplygraph.application.security.UserManagementService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@PageTitle("User & Role Governance | ORBIX")
@Route(value = "admin/users", layout = MainLayout.class)
public class UserRoleAdministrationView extends PlatformAdminViewBase {

    private final UserManagementService userService;
    private final Grid<UserProfile> grid = new Grid<>();

    public UserRoleAdministrationView(UserManagementService userService) {
        super("User & Role Governance", "Manage enterprise identities, Role-Based Access Controls (RBAC), and tenant segregation.");
        this.userService = userService;

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setWidthFull();

        Button btnCreate = new Button("Provision Enterprise User", VaadinIcon.USER_CHECK.create(), e -> openCreateUserDialog());
        btnCreate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionsBar.add(btnCreate);
        add(actionsBar);

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(p -> p.id().value()).setHeader("User ID").setAutoWidth(true);
        grid.addColumn(UserProfile::username).setHeader("Username");
        grid.addColumn(UserProfile::displayName).setHeader("Display Name");
        grid.addColumn(UserProfile::email).setHeader("Email");
        grid.addColumn(UserProfile::role).setHeader("Assigned Role");
        grid.addColumn(p -> p.active() ? "ACTIVE" : "INACTIVE").setHeader("Status");

        grid.addComponentColumn(user -> {
            HorizontalLayout row = new HorizontalLayout();

            Button btnToggle = new Button(user.active() ? "Deactivate" : "Activate", e -> {
                try {
                    userService.toggleUserStatus(user.id(), !user.active());
                    refreshGrid();
                    Notification.show("User status updated", 2000, Notification.Position.TOP_CENTER);
                } catch (Exception ex) {
                    Notification.show("Failed: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnToggle.addThemeVariants(ButtonVariant.LUMO_SMALL, user.active() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);
            row.add(btnToggle);

            return row;
        }).setHeader("Actions");

        grid.setHeight("450px");
    }

    private void refreshGrid() {
        List<UserProfile> users = userService.listAllUsers();
        grid.setItems(users);
    }

    private void openCreateUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Provision Enterprise User");

        VerticalLayout layout = new VerticalLayout();
        TextField tfUsername = new TextField("Username");
        TextField tfDisplayName = new TextField("Full Display Name");
        TextField tfEmail = new TextField("Email Address");
        ComboBox<UserRole> cbRole = new ComboBox<>("Enterprise Role");
        cbRole.setItems(UserRole.values());
        cbRole.setValue(UserRole.SUPPLY_CHAIN_MANAGER);

        layout.add(tfUsername, tfDisplayName, tfEmail, cbRole);
        dialog.add(layout);

        Button btnSave = new Button("Provision", e -> {
            try {
                userService.createUser(
                        "usr-" + UUID.randomUUID().toString().substring(0, 8),
                        tfUsername.getValue(),
                        tfEmail.getValue(),
                        tfDisplayName.getValue(),
                        cbRole.getValue(),
                        Set.of(currentWorkspaceId())
                );
                refreshGrid();
                dialog.close();
                Notification.show("User provisioned successfully", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Failed to provision user: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(btnCancel, btnSave);
        dialog.open();
    }
}
