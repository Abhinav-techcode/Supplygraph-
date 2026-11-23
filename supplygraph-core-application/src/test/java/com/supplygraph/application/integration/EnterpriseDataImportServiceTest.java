package com.supplygraph.application.integration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.ImportJobRepositoryPort;
import com.supplygraph.application.provenance.DataProvenanceService;
import com.supplygraph.domain.model.integration.DataCategory;
import com.supplygraph.domain.model.integration.ImportJob;
import com.supplygraph.domain.model.integration.ImportJobId;
import com.supplygraph.domain.model.integration.ImportSourceType;
import com.supplygraph.domain.model.integration.ImportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Enterprise Data Import Service Workflow Test")
class EnterpriseDataImportServiceTest {

    private ImportJobRepositoryPort importJobRepository;
    private DigitalTwinRepositoryPort digitalTwinRepository;
    private DataProvenanceService provenanceService;
    private AuditGovernanceService auditGovernanceService;
    private EnterpriseDataImportService importService;

    private final Map<ImportJobId, ImportJob> storage = new HashMap<>();

    @BeforeEach
    void setUp() {
        importJobRepository = mock(ImportJobRepositoryPort.class);
        digitalTwinRepository = mock(DigitalTwinRepositoryPort.class);
        provenanceService = mock(DataProvenanceService.class);
        auditGovernanceService = mock(AuditGovernanceService.class);

        doAnswer(invocation -> {
            ImportJob job = invocation.getArgument(0);
            storage.put(job.id(), job);
            return null;
        }).when(importJobRepository).save(any(ImportJob.class));

        doAnswer(invocation -> {
            ImportJobId id = invocation.getArgument(0);
            return Optional.ofNullable(storage.get(id));
        }).when(importJobRepository).findById(any(ImportJobId.class));

        importService = new EnterpriseDataImportService(
                importJobRepository,
                digitalTwinRepository,
                provenanceService,
                auditGovernanceService
        );
    }

    @Test
    @DisplayName("Should parse, validate, and execute valid supplier CSV dataset")
    void shouldParseValidateAndExecuteValidCsv() throws IOException {
        String csv = """
                supplier_id,name,lead_time
                SUP-FAB-01,Munich Semiconductor Fab,60
                SUP-FAB-02,Dresden Silicon Foundry,45
                """;

        ImportJob job = importService.parseAndValidate(
                csv.getBytes(StandardCharsets.UTF_8),
                ImportSourceType.CSV,
                DataCategory.SUPPLIER,
                "suppliers_q4.csv",
                "WS-TEST-01",
                "USR-SCM-01"
        );

        assertThat(job.status()).isEqualTo(ImportStatus.VALIDATED);
        assertThat(job.totalRecords()).isEqualTo(2);
        assertThat(job.acceptedRecords()).isEqualTo(2);
        assertThat(job.rejectedRecords()).isEqualTo(0);
        assertThat(job.failures()).isEmpty();

        // Execute import
        ImportJob completedJob = importService.executeImport(job.id(), "WS-TEST-01", "USR-SCM-01");
        assertThat(completedJob.status()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(completedJob.acceptedRecords()).isEqualTo(2);

        verify(provenanceService, times(2)).registerProvenance(any(), any(), any(), any(), any(), any(), any());
        verify(auditGovernanceService, times(1)).logAction(any(), any(), eq(com.supplygraph.domain.model.audit.AuditAction.DATA_IMPORTED), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should detect domain validation errors and duplicate keys in CSV dataset")
    void shouldDetectDuplicateKeysAndValidationErrors() throws IOException {
        String csv = """
                supplier_id,name,lead_time
                SUP-01,Supplier One,-10
                SUP-01,Duplicate Supplier One,30
                ,Unnamed Missing ID,15
                """;

        ImportJob job = importService.parseAndValidate(
                csv.getBytes(StandardCharsets.UTF_8),
                ImportSourceType.CSV,
                DataCategory.SUPPLIER,
                "bad_suppliers.csv",
                "WS-TEST-01",
                "USR-SCM-01"
        );

        assertThat(job.totalRecords()).isEqualTo(3);
        assertThat(job.rejectedRecords()).isGreaterThan(0);
        assertThat(job.failures()).isNotEmpty();

        boolean hasNegativeLt = job.failures().stream().anyMatch(f -> f.errorCode().equals("NEGATIVE_VALUE"));
        boolean hasDuplicate = job.failures().stream().anyMatch(f -> f.errorCode().equals("DUPLICATE_KEY"));
        boolean hasMissingId = job.failures().stream().anyMatch(f -> f.errorCode().equals("MISSING_REQUIRED_FIELD"));

        assertThat(hasNegativeLt).isTrue();
        assertThat(hasDuplicate).isTrue();
        assertThat(hasMissingId).isTrue();
    }

    @Test
    @DisplayName("Should reject files exceeding 10MB threshold")
    void shouldRejectOversizedFiles() {
        byte[] oversized = new byte[11 * 1024 * 1024];

        assertThatThrownBy(() -> importService.parseAndValidate(
                oversized,
                ImportSourceType.CSV,
                DataCategory.INVENTORY,
                "giant.csv",
                "WS-TEST-01",
                "USR-01"
        )).isInstanceOf(ValidationException.class)
          .hasMessageContaining("exceeds maximum permitted threshold");
    }
}
