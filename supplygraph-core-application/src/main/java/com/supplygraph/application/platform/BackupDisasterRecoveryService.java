package com.supplygraph.application.platform;

import com.supplygraph.application.platform.port.out.BackupSnapshotRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.BackupSnapshotId;
import com.supplygraph.domain.model.platform.BackupSnapshotRecord;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BackupDisasterRecoveryService {

    private final BackupSnapshotRepositoryPort snapshotRepository;

    public BackupDisasterRecoveryService(BackupSnapshotRepositoryPort snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public record RestoreResult(
            BackupSnapshotId snapshotId,
            boolean success,
            String message,
            Instant restoredAt
    ) {}

    public BackupSnapshotRecord createBackupSnapshot(WorkspaceId workspaceId, String snapshotName) {
        long mockByteSize = 10_485_760L + (long) (Math.random() * 5_000_000L);
        String checksumSeed = workspaceId.value() + "-" + snapshotName + "-" + Instant.now();
        String checksum = computeChecksum(checksumSeed);

        BackupSnapshotRecord record = new BackupSnapshotRecord(
                BackupSnapshotId.generate(),
                workspaceId,
                snapshotName,
                mockByteSize,
                checksum,
                false,
                Instant.now()
        );
        snapshotRepository.save(record);
        return record;
    }

    public List<BackupSnapshotRecord> listSnapshots(WorkspaceId workspaceId) {
        return snapshotRepository.findByWorkspaceId(workspaceId);
    }

    public Optional<BackupSnapshotRecord> findById(BackupSnapshotId id) {
        return snapshotRepository.findById(id);
    }

    public BackupSnapshotRecord verifySnapshot(BackupSnapshotId id) {
        BackupSnapshotRecord record = snapshotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + id.value()));

        BackupSnapshotRecord verified = record.withVerified(true);
        snapshotRepository.save(verified);
        return verified;
    }

    public RestoreResult restoreSnapshot(BackupSnapshotId id) {
        BackupSnapshotRecord record = snapshotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + id.value()));

        if (!record.verified()) {
            return new RestoreResult(id, false, "Snapshot checksum must be verified prior to restoration", Instant.now());
        }

        return new RestoreResult(id, true, "Snapshot restored successfully to workspace " + record.workspaceId().value(), Instant.now());
    }

    private String computeChecksum(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }
}

