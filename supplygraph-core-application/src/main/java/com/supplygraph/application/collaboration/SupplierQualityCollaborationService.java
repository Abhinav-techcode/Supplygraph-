package com.supplygraph.application.collaboration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.collaboration.port.out.SupplierQualityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.EightDStep;
import com.supplygraph.domain.model.collaboration.IncidentSeverity;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.QualityIncident8DId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierQualityCollaborationService {

    private final SupplierQualityRepositoryPort qualityRepository;

    public SupplierQualityCollaborationService(SupplierQualityRepositoryPort qualityRepository) {
        this.qualityRepository = qualityRepository;
    }

    public QualityIncident8D openIncident(
            WorkspaceId workspaceId,
            PartnerOrganizationId supplierId,
            MaterialId materialId,
            String defectTitle,
            IncidentSeverity severity
    ) {
        QualityIncident8D incident = new QualityIncident8D(
                QualityIncident8DId.generate(),
                workspaceId,
                supplierId,
                materialId,
                defectTitle,
                severity,
                EightDStep.D1_TEAM_FORMATION,
                false,
                Instant.now()
        );
        qualityRepository.save(incident);
        return incident;
    }

    public QualityIncident8D advanceStep(
            WorkspaceId workspaceId,
            QualityIncident8DId incidentId,
            EightDStep nextStep
    ) {
        QualityIncident8D incident = qualityRepository.findById(incidentId)
                .orElseThrow(() -> new ValidationException("Quality incident not found: " + incidentId));

        QualityIncident8D advanced = incident.advanceStep(nextStep);
        qualityRepository.save(advanced);
        return advanced;
    }

    public Optional<QualityIncident8D> getIncident(QualityIncident8DId id) {
        return qualityRepository.findById(id);
    }

    public List<QualityIncident8D> listIncidents(WorkspaceId workspaceId) {
        return qualityRepository.findByWorkspaceId(workspaceId);
    }
}

