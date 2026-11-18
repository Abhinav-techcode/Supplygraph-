package com.supplygraph.application.planning;

import com.supplygraph.application.planning.port.out.ProcurementPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.ProcurementPlanId;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcurementPlanningService {

    private final ProcurementPlanRepositoryPort procurementPlanRepository;

    public ProcurementPlanningService(ProcurementPlanRepositoryPort procurementPlanRepository) {
        this.procurementPlanRepository = procurementPlanRepository;
    }

    public NetRequirementItem calculateNetRequirement(
            MaterialId componentId,
            NodeId consumingPlantId,
            double grossRequirement,
            double onHandInventory,
            double scheduledReceipts,
            LocalDate requiredDate
    ) {
        double available = onHandInventory + scheduledReceipts;
        double net = Math.max(0.0, grossRequirement - available);

        return new NetRequirementItem(
                componentId,
                consumingPlantId,
                grossRequirement,
                onHandInventory,
                scheduledReceipts,
                net,
                requiredDate
        );
    }

    public ProcurementPlan generateProcurementPlan(
            WorkspaceId workspaceId,
            String planName,
            List<NetRequirementItem> netRequirements,
            List<SupplierAllocationShare> allocations
    ) {
        BigDecimal totalCost = allocations.stream()
                .map(SupplierAllocationShare::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProcurementPlan plan = new ProcurementPlan(
                ProcurementPlanId.generate(),
                workspaceId,
                planName,
                netRequirements,
                allocations,
                totalCost,
                Instant.now()
        );

        procurementPlanRepository.save(plan);
        return plan;
    }

    public Optional<ProcurementPlan> getProcurementPlan(ProcurementPlanId id) {
        return procurementPlanRepository.findById(id);
    }

    public List<ProcurementPlan> listProcurementPlans(WorkspaceId workspaceId) {
        return procurementPlanRepository.findByWorkspaceId(workspaceId);
    }
}

