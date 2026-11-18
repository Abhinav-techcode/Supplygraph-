package com.supplygraph.application.collaboration;

import com.supplygraph.application.collaboration.port.out.OrderCollaborationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNoticeId;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.PoCollaborationId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class OrderCollaborationService {

    private final OrderCollaborationRepositoryPort orderRepository;

    public OrderCollaborationService(OrderCollaborationRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    public PoCollaborationRecord issuePo(
            WorkspaceId workspaceId,
            String poNumber,
            PartnerOrganizationId supplierId,
            MaterialId materialId,
            double orderQuantity,
            BigDecimal totalAmountUsd,
            LocalDate initialPromisedDate
    ) {
        PoCollaborationRecord po = new PoCollaborationRecord(
                PoCollaborationId.generate(),
                workspaceId,
                poNumber,
                supplierId,
                materialId,
                orderQuantity,
                totalAmountUsd,
                initialPromisedDate,
                PoStatus.ISSUED,
                Instant.now()
        );
        orderRepository.savePo(po);
        return po;
    }

    public PoCollaborationRecord acknowledgePo(
            WorkspaceId workspaceId,
            PoCollaborationId poId,
            LocalDate confirmedPromisedDate
    ) {
        PoCollaborationRecord po = orderRepository.findPoById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));

        PoCollaborationRecord acknowledged = po.withStatus(PoStatus.ACKNOWLEDGED, confirmedPromisedDate);
        orderRepository.savePo(acknowledged);
        return acknowledged;
    }

    public AdvanceShippingNotice dispatchAsn(
            WorkspaceId workspaceId,
            PoCollaborationId poId,
            String trackingNumber,
            String carrierName,
            double shippedQuantity,
            LocalDate estimatedArrivalDate
    ) {
        PoCollaborationRecord po = orderRepository.findPoById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));

        AdvanceShippingNotice asn = new AdvanceShippingNotice(
                AdvanceShippingNoticeId.generate(),
                workspaceId,
                poId,
                trackingNumber,
                carrierName,
                shippedQuantity,
                estimatedArrivalDate,
                Instant.now()
        );

        orderRepository.saveAsn(asn);

        PoCollaborationRecord shippedPo = po.withStatus(PoStatus.SHIPPED, po.promisedDeliveryDate());
        orderRepository.savePo(shippedPo);

        return asn;
    }

    public Optional<PoCollaborationRecord> getPo(PoCollaborationId id) {
        return orderRepository.findPoById(id);
    }

    public List<PoCollaborationRecord> listPos(WorkspaceId workspaceId) {
        return orderRepository.findPosByWorkspaceId(workspaceId);
    }

    public List<AdvanceShippingNotice> listAsns(WorkspaceId workspaceId) {
        return orderRepository.findAsnsByWorkspaceId(workspaceId);
    }
}

