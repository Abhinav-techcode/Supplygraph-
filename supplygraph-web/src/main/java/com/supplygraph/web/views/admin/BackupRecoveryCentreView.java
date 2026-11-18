package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.BackupDisasterRecoveryService;
import com.supplygraph.domain.model.platform.BackupSnapshotRecord;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@PageTitle("Disaster Recovery Vault | SupplyGraph")
@Route(value = "admin/backup", layout = MainLayout.class)
public class BackupRecoveryCentreView extends PlatformAdminViewBase {

    private final BackupDisasterRecoveryService backupService;
    private final Grid<BackupSnapshotRecord> grid = new Grid<>();

    public BackupRecoveryCentreView(BackupDisasterRecoveryService backupService) {
        super("Disaster Recovery & Backup Vault", "Cryptographically verifiable database state snapshots, immutable point-in-time recovery, and restore validation.");
        this.backupService = backupService;

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setWidthFull();

        Button btnCreate = new Button("Create Snapshot Backup", VaadinIcon.DATABASE.create(), e -> openCreateSnapshotDialog());
        btnCreate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionsBar.add(btnCreate);
        add(actionsBar);

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(s -> s.id().value()).setHeader("Snapshot ID").setAutoWidth(true);
        grid.addColumn(BackupSnapshotRecord::snapshotName).setHeader("Snapshot Name").setAutoWidth(true);
        grid.addColumn(s -> (s.byteSize() / 1024 / 1024) + " MB").setHeader("Storage Size");
        grid.addColumn(s -> s.storageChecksum().length() > 24 ? s.storageChecksum().substring(0, 24) + "..." : s.storageChecksum()).setHeader("SHA-256 Checksum");
        grid.addColumn(s -> s.verified() ? "VERIFIED" : "UNVERIFIED").setHeader("Integrity Status");
        grid.addColumn(BackupSnapshotRecord::createdAt).setHeader("Created At");

        grid.addComponentColumn(s -> {
            HorizontalLayout row = new HorizontalLayout();

            if (!s.verified()) {
                Button btnVerify = new Button("Verify Integrity", e -> {
                    backupService.verifySnapshot(s.id());
                    refreshGrid();
                    Notification.show("Checksum verified successfully", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                });
                btnVerify.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                row.add(btnVerify);
            }

            Button btnRestore = new Button("Restore", e -> {
                var res = backupService.restoreSnapshot(s.id());
                if (res.success()) {
                    Notification.show(res.message(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show(res.message(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnRestore.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            row.add(btnRestore);

            return row;
        }).setHeader("Actions");

        grid.setHeight("400px");
    }

    private void refreshGrid() {
        List<BackupSnapshotRecord> snapshots = backupService.listSnapshots(currentWorkspaceId());
        grid.setItems(snapshots);
    }

    private void openCreateSnapshotDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Disaster Recovery Snapshot");

        VerticalLayout layout = new VerticalLayout();
        TextField tfName = new TextField("Snapshot Label");
        tfName.setPlaceholder("e.g. pre-upgrade-snap-01");
        tfName.setWidthFull();

        layout.add(tfName);
        dialog.add(layout);

        Button btnSave = new Button("Capture Snapshot", e -> {
            if (tfName.getValue() == null || tfName.getValue().isBlank()) {
                Notification.show("Please enter a snapshot label", 2000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                backupService.createBackupSnapshot(currentWorkspaceId(), tfName.getValue());
                refreshGrid();
                dialog.close();
                Notification.show("Disaster recovery snapshot captured successfully", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Snapshot creation failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(btnCancel, btnSave);
        dialog.open();
    }
}
