package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.collaboration.AdvanceShippingNoticeJpaEntity;
import com.supplygraph.adapter.persistence.entity.collaboration.PoCollaborationJpaEntity;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataAdvanceShippingNoticeRepository;
import com.supplygraph.adapter.persistence.repository.collaboration.SpringDataPoCollaborationRepository;
import com.supplygraph.application.collaboration.port.out.OrderCollaborationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNoticeId;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.PoCollaborationId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresOrderCollaborationPersistenceAdapter implements OrderCollaborationRepositoryPort {

    private final SpringDataPoCollaborationRepository poRepository;
    private final SpringDataAdvanceShippingNoticeRepository asnRepository;

    public PostgresOrderCollaborationPersistenceAdapter(
            SpringDataPoCollaborationRepository poRepository,
            SpringDataAdvanceShippingNoticeRepository asnRepository
    ) {
        this.poRepository = poRepository;
        this.asnRepository = asnRepository;
    }

    @Override
    @Transactional
    public void savePo(PoCollaborationRecord po) {
        PoCollaborationJpaEntity entity = new PoCollaborationJpaEntity(
                po.id().value(),
                po.workspaceId().value(),
                po.poNumber(),
                po.supplierId().value(),
                po.materialId().value(),
                BigDecimal.valueOf(po.orderQuantity()),
                po.totalAmountUsd(),
                po.promisedDeliveryDate(),
                po.status().name(),
                po.lastUpdated()
        );
        poRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PoCollaborationRecord> findPoById(PoCollaborationId id) {
        return poRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoCollaborationRecord> findPosByWorkspaceId(WorkspaceId workspaceId) {
        return poRepository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveAsn(AdvanceShippingNotice asn) {
        AdvanceShippingNoticeJpaEntity entity = new AdvanceShippingNoticeJpaEntity(
                asn.id().value(),
                asn.workspaceId().value(),
                asn.poId().value(),
                asn.trackingNumber(),
                asn.carrierName(),
                BigDecimal.valueOf(asn.shippedQuantity()),
                asn.estimatedArrivalDate(),
                asn.dispatchedAt()
        );
        asnRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdvanceShippingNotice> findAsnById(AdvanceShippingNoticeId id) {
        return asnRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvanceShippingNotice> findAsnsByWorkspaceId(WorkspaceId workspaceId) {
        return asnRepository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private PoCollaborationRecord toDomain(PoCollaborationJpaEntity e) {
        return new PoCollaborationRecord(
                new PoCollaborationId(e.getId()),
                new WorkspaceId(e.getWorkspaceId()),
                e.getPoNumber(),
                new PartnerOrganizationId(e.getSupplierId()),
                new MaterialId(e.getMaterialId()),
                e.getOrderQuantity().doubleValue(),
                e.getTotalAmountUsd(),
                e.getPromisedDeliveryDate(),
                PoStatus.valueOf(e.getStatus()),
                e.getLastUpdated()
        );
    }

    private AdvanceShippingNotice toDomain(AdvanceShippingNoticeJpaEntity e) {
        return new AdvanceShippingNotice(
                new AdvanceShippingNoticeId(e.getId()),
                new WorkspaceId(e.getWorkspaceId()),
                new PoCollaborationId(e.getPoId()),
                e.getTrackingNumber(),
                e.getCarrierName(),
                e.getShippedQuantity().doubleValue(),
                e.getEstimatedArrivalDate(),
                e.getDispatchedAt()
        );
    }
}

