package com.supplygraph.application.planning;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.planning.port.out.InventoryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.InventoryPlanId;
import com.supplygraph.domain.model.planning.SafetyStockResult;
import com.supplygraph.domain.model.planning.ServiceLevel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MultiEchelonInventoryService {

    private final InventoryPlanRepositoryPort inventoryPlanRepository;

    public MultiEchelonInventoryService(InventoryPlanRepositoryPort inventoryPlanRepository) {
        this.inventoryPlanRepository = inventoryPlanRepository;
    }

    public SafetyStockResult calculateSafetyStock(
            double meanDailyDemand,
            double stdDevDailyDemand,
            int leadTimeDays,
            double stdDevLeadTimeDays,
            ServiceLevel serviceLevel,
            double annualHoldingCostPerUnit,
            double fixedOrderingCost
    ) {
        if (meanDailyDemand < 0 || stdDevDailyDemand < 0 || leadTimeDays < 1) {
            throw new ValidationException("Demand and lead time parameters must be non-negative");
        }

        double z = serviceLevel.getZScore();

        // Silver-Pyke variance decomposition: Var(LTD) = L * Var(D) + D^2 * Var(L)
        double demandVariabilityComponent = leadTimeDays * (stdDevDailyDemand * stdDevDailyDemand);
        double leadTimeVariabilityComponent = (meanDailyDemand * meanDailyDemand) * (stdDevLeadTimeDays * stdDevLeadTimeDays);
        double stdDevLeadTimeDemand = Math.sqrt(demandVariabilityComponent + leadTimeVariabilityComponent);

        double baseSafetyStock = z * Math.sqrt(leadTimeDays) * stdDevDailyDemand;
        double recommendedSafetyStock = Math.ceil(z * stdDevLeadTimeDemand);
        double leadTimeBuffer = Math.max(0.0, recommendedSafetyStock - baseSafetyStock);
        double demandBuffer = baseSafetyStock;

        double reorderPoint = Math.ceil((meanDailyDemand * leadTimeDays) + recommendedSafetyStock);

        // Economic Order Quantity (EOQ): sqrt(2 * AnnualDemand * SetupCost / HoldingCost)
        double annualDemand = meanDailyDemand * 365.0;
        double eoq = 0.0;
        if (annualHoldingCostPerUnit > 0 && annualDemand > 0) {
            eoq = Math.ceil(Math.sqrt((2.0 * annualDemand * fixedOrderingCost) / annualHoldingCostPerUnit));
        }

        return new SafetyStockResult(
                baseSafetyStock,
                leadTimeBuffer,
                demandBuffer,
                recommendedSafetyStock,
                reorderPoint,
                eoq
        );
    }

    public InventoryPlan generateInventoryPlan(
            WorkspaceId workspaceId,
            String planName,
            ServiceLevel serviceLevel,
            List<EchelonInventoryPosition> positions
    ) {
        double totalCarryingCost = positions.stream()
                .mapToDouble(EchelonInventoryPosition::calculateAnnualCarryingCost)
                .sum();

        InventoryPlan plan = new InventoryPlan(
                InventoryPlanId.generate(),
                workspaceId,
                planName,
                serviceLevel,
                positions,
                totalCarryingCost,
                Instant.now()
        );

        inventoryPlanRepository.save(plan);
        return plan;
    }

    public Optional<InventoryPlan> getInventoryPlan(InventoryPlanId id) {
        return inventoryPlanRepository.findById(id);
    }

    public List<InventoryPlan> listInventoryPlans(WorkspaceId workspaceId) {
        return inventoryPlanRepository.findByWorkspaceId(workspaceId);
    }
}

