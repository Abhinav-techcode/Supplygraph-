package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.collaboration.RfqDocumentJpaEntity;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataRfqRepository;
import com.supplygraph.application.collaboration.port.out.RfqRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqId;
import com.supplygraph.domain.model.collaboration.RfqQuotation;
import com.supplygraph.domain.model.collaboration.RfqStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresRfqPersistenceAdapter implements RfqRepositoryPort {

    private final SpringDataRfqRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresRfqPersistenceAdapter(SpringDataRfqRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void save(RfqDocument rfq) {
        try {
            String json = objectMapper.writeValueAsString(rfq.quotations());
            RfqDocumentJpaEntity entity = new RfqDocumentJpaEntity(
                    rfq.id().value(),
                    rfq.workspaceId().value(),
                    rfq.title(),
                    rfq.targetMaterialId().value(),
                    BigDecimal.valueOf(rfq.targetQuantity()),
                    rfq.submissionDeadline(),
                    rfq.status().name(),
                    json,
                    rfq.createdAt()
            );
            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize RFQ quotations", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RfqDocument> findById(RfqId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfqDocument> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private RfqDocument toDomain(RfqDocumentJpaEntity e) {
        try {
            List<RfqQuotation> quotes = objectMapper.readValue(e.getQuotationsJson(), new TypeReference<List<RfqQuotation>>() {});
            return new RfqDocument(
                    new RfqId(e.getId()),
                    new WorkspaceId(e.getWorkspaceId()),
                    e.getTitle(),
                    new MaterialId(e.getTargetMaterialId()),
                    e.getTargetQuantity().doubleValue(),
                    e.getSubmissionDeadline(),
                    RfqStatus.valueOf(e.getStatus()),
                    quotes != null ? quotes : Collections.emptyList(),
                    e.getCreatedAt()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize RFQ quotations", ex);
        }
    }
}

