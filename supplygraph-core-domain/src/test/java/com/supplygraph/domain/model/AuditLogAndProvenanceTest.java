package com.supplygraph.domain.model;

import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.audit.AuditLogId;
import com.supplygraph.domain.model.provenance.ProvenanceId;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.domain.model.security.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogAndProvenanceTest {

    @Test
    void auditLogEntry_calculatesAndVerifiesCryptographicSignature() {
        AuditLogEntry entry = AuditLogEntry.record(
                UserId.of("USR-MGR-1"),
                WorkspaceId.of("WS-AUTO-01"),
                AuditAction.PLAN_APPROVED.name(),
                "PLAN-RECOVER-88",
                "V1",
                "V2",
                "Expedited supplier batch approved to avoid battery cell stockout",
                "CONF-NONE"
        );

        assertThat(entry.id()).isNotNull();
        assertThat(entry.signatureHash()).isNotBlank();
        assertThat(entry.isSignatureValid()).isTrue();

        // Tamper test: mutating any canonical field invalidates signature
        AuditLogEntry tampered = new AuditLogEntry(
                entry.id(),
                UserId.of("MALICIOUS-ACTOR"), // tampered user
                entry.workspaceId(),
                entry.action(),
                entry.targetEntityId(),
                entry.previousStateVersion(),
                entry.newStateVersion(),
                entry.timestamp(),
                entry.rationale(),
                entry.concurGuardConflictRef(),
                entry.signatureHash()
        );

        assertThat(tampered.isSignatureValid()).isFalse();
    }

    @Test
    void provenanceRecord_tracksAssetClassificationAndMetadata() {
        ProvenanceRecord record = ProvenanceRecord.of(
                WorkspaceId.of("WS-AUTO-01"),
                "BATTERY-CELL-NMC",
                "MATERIAL",
                ProvenanceType.REAL_IMPORTED,
                "SAP-ERP-EXTRACT-2026-11-17",
                "V2",
                Map.of("supplier", "CATL-MUNICH", "confidence", "1.0")
        );

        assertThat(record.id()).isNotNull();
        assertThat(record.type()).isEqualTo(ProvenanceType.REAL_IMPORTED);
        assertThat(record.type().isAuthoritative()).isTrue();
        assertThat(record.attributes()).containsEntry("supplier", "CATL-MUNICH");

        ProvenanceRecord aiRecord = ProvenanceRecord.of(
                WorkspaceId.of("WS-AUTO-01"),
                "ACTION-PROP-42",
                "PROPOSAL",
                ProvenanceType.AI_PROPOSAL,
                "LOGISTICS_AGENT_V1",
                "V2",
                Map.of("suggestedCarrier", "DHL_EXPRESS")
        );

        assertThat(aiRecord.type().isAuthoritative()).isFalse();
    }
}

