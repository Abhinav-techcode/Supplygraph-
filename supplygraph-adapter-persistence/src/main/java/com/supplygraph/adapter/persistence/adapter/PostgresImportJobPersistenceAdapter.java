package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.ImportJobFailureJpaEntity;
import com.supplygraph.adapter.persistence.entity.ImportJobJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataImportJobRepository;
import com.supplygraph.application.port.out.ImportJobRepositoryPort;
import com.supplygraph.domain.model.integration.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresImportJobPersistenceAdapter implements ImportJobRepositoryPort {

    private final SpringDataImportJobRepository repository;

    public PostgresImportJobPersistenceAdapter(SpringDataImportJobRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(ImportJob job) {
        ImportJobJpaEntity entity = repository.findById(job.id().value()).orElseGet(ImportJobJpaEntity::new);
        entity.setId(job.id().value());
        entity.setWorkspaceId(job.workspaceId());
        entity.setSourceType(job.sourceType().name());
        entity.setCategory(job.category().name());
        entity.setFilename(job.filename());
        entity.setFileChecksum(job.fileChecksum());
        entity.setSchemaVersion(job.schemaVersion());
        entity.setTotalRecords(job.totalRecords());
        entity.setAcceptedRecords(job.acceptedRecords());
        entity.setRejectedRecords(job.rejectedRecords());
        entity.setStatus(job.status().name());
        entity.setImportedBy(job.importedBy());
        entity.setCreatedAt(job.createdAt());
        entity.setCompletedAt(job.completedAt());

        entity.getFailures().clear();
        for (ImportRecordFailure failure : job.failures()) {
            ImportJobFailureJpaEntity fe = new ImportJobFailureJpaEntity();
            fe.setJob(entity);
            fe.setRowIndex(failure.rowIndex());
            fe.setFieldName(failure.field());
            fe.setRejectedValue(failure.rejectedValue());
            fe.setErrorCode(failure.errorCode());
            fe.setMessage(failure.message());
            fe.setSuggestedCorrection(failure.suggestedCorrection());
            entity.getFailures().add(fe);
        }

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJob> findById(ImportJobId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJob> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ImportJob toDomain(ImportJobJpaEntity entity) {
        List<ImportRecordFailure> failures = new ArrayList<>();
        if (entity.getFailures() != null) {
            for (ImportJobFailureJpaEntity f : entity.getFailures()) {
                failures.add(new ImportRecordFailure(
                        f.getRowIndex(),
                        f.getFieldName(),
                        f.getRejectedValue(),
                        f.getErrorCode(),
                        f.getMessage(),
                        f.getSuggestedCorrection()
                ));
            }
        }

        return new ImportJob(
                ImportJobId.of(entity.getId()),
                entity.getWorkspaceId(),
                ImportSourceType.valueOf(entity.getSourceType()),
                DataCategory.valueOf(entity.getCategory()),
                entity.getFilename(),
                entity.getFileChecksum(),
                entity.getSchemaVersion(),
                entity.getTotalRecords(),
                entity.getAcceptedRecords(),
                entity.getRejectedRecords(),
                failures,
                ImportStatus.valueOf(entity.getStatus()),
                entity.getImportedBy(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}

