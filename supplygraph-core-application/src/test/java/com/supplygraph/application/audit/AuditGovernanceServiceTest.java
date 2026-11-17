package com.supplygraph.application.audit;

import com.supplygraph.application.port.out.AuditLogRepositoryPort;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.audit.AuditLogId;
import com.supplygraph.domain.model.security.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditGovernanceServiceTest {

    private AuditLogRepositoryPort auditLogRepository;
    private WorkspaceAccessGuard workspaceAccessGuard;
    private AuditGovernanceService auditService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepositoryPort.class);
        workspaceAccessGuard = mock(WorkspaceAccessGuard.class);
        auditService = new AuditGovernanceService(auditLogRepository, workspaceAccessGuard);
    }

    @Test
    void logAction_createsImmutableEntryWithValidSignature() {
        when(auditLogRepository.save(any(AuditLogEntry.class))).thenAnswer(i -> i.getArgument(0));

        AuditLogEntry entry = auditService.logAction(
                UserId.of("MGR-1"),
                WorkspaceId.of("WS-AUTO-01"),
                AuditAction.PLAN_APPROVED,
                "PLAN-RECOVER-101",
                "V1",
                "V2",
                "Expedited shipping approved",
                "CONF-01"
        );

        assertThat(entry).isNotNull();
        assertThat(entry.signatureHash()).isNotBlank();
        assertThat(entry.isSignatureValid()).isTrue();

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PLAN_APPROVED");
    }

    @Test
    void verifyIntegrity_checksCryptographicSignature() {
        AuditLogEntry validEntry = AuditLogEntry.record(
                UserId.of("MGR-1"),
                WorkspaceId.of("WS-1"),
                "PLAN_APPROVED",
                "PLAN-1",
                "V1",
                "V2",
                "Approved",
                "NONE"
        );

        when(auditLogRepository.findById(validEntry.id())).thenReturn(Optional.of(validEntry));

        boolean isValid = auditService.verifyIntegrity(validEntry.id());
        assertThat(isValid).isTrue();
    }
}

