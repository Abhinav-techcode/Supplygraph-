package com.supplygraph.application.collaboration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.collaboration.port.out.PartnerDocumentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.DocumentCategory;
import com.supplygraph.domain.model.collaboration.PartnerDocumentId;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PartnerDocumentExchangeService {

    private final PartnerDocumentRepositoryPort documentRepository;

    public PartnerDocumentExchangeService(PartnerDocumentRepositoryPort documentRepository) {
        this.documentRepository = documentRepository;
    }

    public PartnerDocumentRecord uploadDocument(
            WorkspaceId workspaceId,
            PartnerOrganizationId partnerId,
            String fileName,
            DocumentCategory category,
            String cryptographicHash
    ) {
        PartnerDocumentRecord doc = new PartnerDocumentRecord(
                PartnerDocumentId.generate(),
                workspaceId,
                partnerId,
                fileName,
                category,
                cryptographicHash,
                false,
                Instant.now()
        );
        documentRepository.save(doc);
        return doc;
    }

    public PartnerDocumentRecord verifyDocument(
            WorkspaceId workspaceId,
            PartnerDocumentId documentId
    ) {
        PartnerDocumentRecord doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ValidationException("Document not found: " + documentId));

        PartnerDocumentRecord verified = doc.withVerified(true);
        documentRepository.save(verified);
        return verified;
    }

    public Optional<PartnerDocumentRecord> getDocument(PartnerDocumentId id) {
        return documentRepository.findById(id);
    }

    public List<PartnerDocumentRecord> listDocuments(WorkspaceId workspaceId) {
        return documentRepository.findByWorkspaceId(workspaceId);
    }

    public List<PartnerDocumentRecord> listPartnerDocuments(WorkspaceId workspaceId, PartnerOrganizationId partnerId) {
        return documentRepository.findByPartnerId(workspaceId, partnerId);
    }
}

