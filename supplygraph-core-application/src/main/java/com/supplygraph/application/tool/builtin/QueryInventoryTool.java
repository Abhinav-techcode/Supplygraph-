package com.supplygraph.application.tool.builtin;

import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Read-only tool allowing bounded agents to query inventory balances across the digital twin.
 */
public class QueryInventoryTool implements AgentTool {

    public static final String NAME = "query_inventory";

    private final Supplier<DigitalTwinSnapshot> snapshotSupplier;

    public QueryInventoryTool(Supplier<DigitalTwinSnapshot> snapshotSupplier) {
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier, "snapshotSupplier must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Query authoritative inventory balances for a given material across digital twin locations.";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        Object matIdObj = request.parameters().get("materialId");
        if (matIdObj == null || matIdObj.toString().isBlank()) {
            return ToolExecutionResult.failure("Missing required parameter: materialId");
        }
        String targetMaterialId = matIdObj.toString();
        String targetNodeId = request.parameters().get("nodeId") != null ? request.parameters().get("nodeId").toString() : null;

        DigitalTwinSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null) {
            return ToolExecutionResult.failure("Digital twin snapshot is not available");
        }

        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        String uom = null;

        Map<String, BigDecimal> breakdownByNode = new HashMap<>();

        for (Map.Entry<InventoryAccountId, Quantity> entry : snapshot.inventoryBalances().entrySet()) {
            InventoryAccountId account = entry.getKey();
            Quantity qty = entry.getValue();

            if (account.materialId().value().equalsIgnoreCase(targetMaterialId)) {
                if (targetNodeId == null || account.nodeId().value().equalsIgnoreCase(targetNodeId)) {
                    uom = qty.unit().name();
                    BigDecimal amount = qty.amount();

                    switch (account.category()) {
                        case ON_HAND_AVAILABLE -> {
                            totalAvailable = totalAvailable.add(amount);
                            breakdownByNode.merge(account.nodeId().value(), amount, BigDecimal::add);
                        }
                        case RESERVED_PRODUCTION, RESERVED_SHIPMENT -> totalReserved = totalReserved.add(amount);
                        default -> {}
                    }
                }
            }
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("materialId", targetMaterialId);
        responseData.put("availableQuantity", totalAvailable.toPlainString());
        responseData.put("reservedQuantity", totalReserved.toPlainString());
        responseData.put("unitOfMeasure", uom != null ? uom : "PIECE");
        responseData.put("nodeBreakdown", breakdownByNode);
        responseData.put("snapshotVersion", snapshot.version().value());

        return ToolExecutionResult.success(responseData);
    }
}
