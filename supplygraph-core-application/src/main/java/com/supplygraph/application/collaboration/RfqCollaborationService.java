package com.supplygraph.application.collaboration;

import com.supplygraph.application.collaboration.port.out.RfqRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.QuotationStatus;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqId;
import com.supplygraph.domain.model.collaboration.RfqQuotation;
import com.supplygraph.domain.model.collaboration.RfqQuotationId;
import com.supplygraph.domain.model.collaboration.RfqStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class RfqCollaborationService {

    private final RfqRepositoryPort rfqRepository;

    public RfqCollaborationService(RfqRepositoryPort rfqRepository) {
        this.rfqRepository = rfqRepository;
    }

    public RfqDocument createRfq(
            WorkspaceId workspaceId,
            String title,
            MaterialId targetMaterialId,
            double targetQuantity,
            LocalDate submissionDeadline
    ) {
        RfqDocument doc = new RfqDocument(
                RfqId.generate(),
                workspaceId,
                title,
                targetMaterialId,
                targetQuantity,
                submissionDeadline,
                RfqStatus.PUBLISHED,
                Collections.emptyList(),
                Instant.now()
        );
        rfqRepository.save(doc);
        return doc;
    }

    public RfqQuotation submitQuotation(
            WorkspaceId workspaceId,
            RfqId rfqId,
            PartnerOrganizationId supplierId,
            BigDecimal unitPriceUsd,
            int leadTimeDays,
            double minOrderQuantity,
            LocalDate validityDate
    ) {
        RfqDocument doc = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new IllegalArgumentException("RFQ document not found: " + rfqId));

        if (doc.status() != RfqStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot submit quotation for RFQ in status: " + doc.status());
        }

        RfqQuotation quote = new RfqQuotation(
                RfqQuotationId.generate(),
                rfqId,
                supplierId,
                unitPriceUsd,
                leadTimeDays,
                minOrderQuantity,
                validityDate,
                QuotationStatus.SUBMITTED,
                Instant.now()
        );

        RfqDocument updated = doc.withQuotation(quote);
        rfqRepository.save(updated);
        return quote;
    }

    public RfqDocument awardQuotation(WorkspaceId workspaceId, RfqId rfqId, RfqQuotationId quoteId) {
        RfqDocument doc = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new IllegalArgumentException("RFQ document not found: " + rfqId));

        List<RfqQuotation> updatedQuotes = new ArrayList<>();
        boolean found = false;

        for (RfqQuotation q : doc.quotations()) {
            if (q.id().equals(quoteId)) {
                updatedQuotes.add(q.withStatus(QuotationStatus.ACCEPTED));
                found = true;
            } else {
                updatedQuotes.add(q.withStatus(QuotationStatus.REJECTED));
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Quotation not found in RFQ: " + quoteId);
        }

        RfqDocument awarded = new RfqDocument(
                doc.id(),
                doc.workspaceId(),
                doc.title(),
                doc.targetMaterialId(),
                doc.targetQuantity(),
                doc.submissionDeadline(),
                RfqStatus.AWARDED,
                updatedQuotes,
                doc.createdAt()
        );

        rfqRepository.save(awarded);
        return awarded;
    }

    public Optional<RfqDocument> getRfq(RfqId rfqId) {
        return rfqRepository.findById(rfqId);
    }

    public List<RfqDocument> listRfqs(WorkspaceId workspaceId) {
        return rfqRepository.findByWorkspaceId(workspaceId);
    }
}

