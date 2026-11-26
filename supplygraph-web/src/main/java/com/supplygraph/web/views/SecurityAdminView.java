package com.supplygraph.web.views;

import com.supplygraph.application.security.UserManagementService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "security-admin", layout = MainLayout.class)
@PageTitle("Security Administration & RBAC | ORBIX")
public class SecurityAdminView extends VerticalLayout {

    private final UserManagementService userService;
    private final Grid<UserProfile> userGrid;

    public SecurityAdminView(UserManagementService userService) {
        this.userService = userService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Security Administration & Role-Based Access Control");
        Paragraph desc = new Paragraph("Enforce server-side workspace isolation, manage user roles, and inspect enterprise permissions.");

        userGrid = new Grid<>();
        userGrid.setSizeFull();

        userGrid.addColumn(u -> u.id().value()).setHeader("User ID").setAutoWidth(true);
        userGrid.addColumn(UserProfile::username).setHeader("Username").setAutoWidth(true);
        userGrid.addColumn(UserProfile::displayName).setHeader("Display Name").setAutoWidth(true);
        userGrid.addColumn(UserProfile::email).setHeader("Email").setAutoWidth(true);

        userGrid.addComponentColumn(user -> {
            Span badge = new Span(user.role().getDisplayName());
            String theme = switch (user.role()) {
                case SYSTEM_ADMIN, WORKSPACE_ADMIN -> "badge primary";
                case SUPPLY_CHAIN_MANAGER -> "badge success";
                case VIEWER -> "badge contrast";
                default -> "badge";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Assigned Role").setAutoWidth(true);

        userGrid.addColumn(u -> u.assignedWorkspaces().stream()
                .map(WorkspaceId::value)
                .collect(Collectors.joining(", "))
        ).setHeader("Assigned Workspaces").setAutoWidth(true);

        userGrid.addComponentColumn(user -> {
            Span status = new Span(user.active() ? "ACTIVE" : "DISABLED");
            status.getElement().getThemeList().add("badge " + (user.active() ? "success" : "error"));
            return status;
        }).setHeader("Status").setAutoWidth(true);

        userGrid.addComponentColumn(user -> {
            ComboBox<UserRole> roleCombo = new ComboBox<>();
            roleCombo.setItems(UserRole.values());
            roleCombo.setItemLabelGenerator(UserRole::getDisplayName);
            roleCombo.setValue(user.role());
            roleCombo.setWidth("180px");

            Button saveBtn = new Button("Update", ev -> {
                if (roleCombo.getValue() != null && roleCombo.getValue() != user.role()) {
                    userService.updateUserRole(user.id(), roleCombo.getValue());
                    Notification.show("Updated role for " + user.username() + " to " + roleCombo.getValue().getDisplayName(), 3000, Notification.Position.BOTTOM_END);
                    refreshUsers();
                }
            });
            saveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button toggleBtn = new Button(user.active() ? "Disable" : "Enable", ev -> {
                userService.toggleUserStatus(user.id(), !user.active());
                Notification.show("User status updated for " + user.username(), 3000, Notification.Position.BOTTOM_END);
                refreshUsers();
            });
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, user.active() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);

            HorizontalLayout actions = new HorizontalLayout(roleCombo, saveBtn, toggleBtn);
            actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        VerticalLayout permissionsMatrixCard = createPermissionsMatrixCard();

        add(title, desc, userGrid, permissionsMatrixCard);
        refreshUsers();
    }

    private VerticalLayout createPermissionsMatrixCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-top", "var(--lumo-space-m)");

        H3 heading = new H3("RBAC Permissions Matrix (9 Roles × 10 Permissions)");
        Paragraph matrixDesc = new Paragraph("Authoritative mapping of granular operation permissions verified on the server-side across all domain use cases.");

        Grid<Permission> matrixGrid = new Grid<>();
        matrixGrid.setAllRowsVisible(true);
        matrixGrid.setItems(Permission.values());

        matrixGrid.addColumn(Permission::name).setHeader("Permission").setAutoWidth(true);
        matrixGrid.addColumn(Permission::getDescription).setHeader("Description").setWidth("320px");

        for (UserRole role : UserRole.values()) {
            matrixGrid.addComponentColumn(perm -> {
                boolean has = role.hasPermission(perm);
                Span indicator = new Span(has ? "✓" : "—");
                indicator.getElement().getThemeList().add("badge " + (has ? "success" : "contrast"));
                return indicator;
            }).setHeader(role.name()).setAutoWidth(true);
        }

        card.add(heading, matrixDesc, matrixGrid);
        return card;
    }

    private void refreshUsers() {
        List<UserProfile> users = userService.listAllUsers();
        userGrid.setItems(users);
    }
}

