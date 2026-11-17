package com.supplygraph.domain.model.audit;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserId;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable, tamper-evident audit log record with cryptographic SHA-256 signature verification.
 */
public record AuditLogEntry(
        AuditLogId id,
        UserId userId,
        WorkspaceId workspaceId,
        String action,
        String targetEntityId,
        String previousStateVersion,
        String newStateVersion,
        Instant timestamp,
        String rationale,
        String concurGuardConflictRef,
        String signatureHash
) implements Serializable {

    public AuditLogEntry {
        Objects.requireNonNull(id, "Audit log ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(action, "Action cannot be null");
        Objects.requireNonNull(targetEntityId, "Target entity ID cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(signatureHash, "Signature hash cannot be null");
    }

    /**
     * Factory method creating a new signed audit log entry.
     */
    public static AuditLogEntry record(
            UserId userId,
            WorkspaceId workspaceId,
            String action,
            String targetEntityId,
            String previousStateVersion,
            String newStateVersion,
            String rationale,
            String concurGuardConflictRef
    ) {
        AuditLogId logId = AuditLogId.generate();
        Instant now = Instant.now();
        String signature = computeSignature(
                logId.value(),
                userId.value(),
                workspaceId.value(),
                action,
                targetEntityId,
                previousStateVersion,
                newStateVersion,
                now,
                rationale,
                concurGuardConflictRef
        );
        return new AuditLogEntry(
                logId,
                userId,
                workspaceId,
                action,
                targetEntityId,
                previousStateVersion,
                newStateVersion,
                now,
                rationale,
                concurGuardConflictRef,
                signature
        );
    }

    /**
     * Computes the SHA-256 tamper-evident hash across the entry's canonical fields.
     */
    public static String computeSignature(
            String id,
            String userId,
            String workspaceId,
            String action,
            String targetEntityId,
            String prevVersion,
            String newVersion,
            Instant timestamp,
            String rationale,
            String conflictRef
    ) {
        String canonical = String.join("|",
                Objects.toString(id, ""),
                Objects.toString(userId, ""),
                Objects.toString(workspaceId, ""),
                Objects.toString(action, ""),
                Objects.toString(targetEntityId, ""),
                Objects.toString(prevVersion, ""),
                Objects.toString(newVersion, ""),
                Objects.toString(timestamp, ""),
                Objects.toString(rationale, ""),
                Objects.toString(conflictRef, "")
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies that the signature hash matches the entry content.
     */
    public boolean isSignatureValid() {
        String expected = computeSignature(
                id.value(),
                userId.value(),
                workspaceId.value(),
                action,
                targetEntityId,
                previousStateVersion,
                newStateVersion,
                timestamp,
                rationale,
                concurGuardConflictRef
        );
        return expected.equalsIgnoreCase(signatureHash);
    }
}

