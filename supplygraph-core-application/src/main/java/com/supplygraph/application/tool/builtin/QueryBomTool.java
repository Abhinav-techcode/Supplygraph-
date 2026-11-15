package com.supplygraph.application.tool.builtin;

import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.BomEntry;
import com.supplygraph.domain.model.bom.Material;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

/**
 * Read-only tool allowing bounded agents to query the multi-tier Bill of Materials (BOM).
 */
public class QueryBomTool implements AgentTool {

    public static final String NAME = "query_bom";

    private final Supplier<DigitalTwinSnapshot> snapshotSupplier;

    public QueryBomTool(Supplier<DigitalTwinSnapshot> snapshotSupplier) {
        this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier, "snapshotSupplier must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Query multi-tier Bill of Materials (BOM) components, scrap yield factors, and exploded requirements.";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        Object matIdObj = request.parameters().get("materialId");
        if (matIdObj == null || matIdObj.toString().isBlank()) {
            return ToolExecutionResult.failure("Missing required parameter: materialId");
        }
        MaterialId targetId = MaterialId.of(matIdObj.toString());

        DigitalTwinSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null) {
            return ToolExecutionResult.failure("Digital twin snapshot is not available");
        }

        BillOfMaterials bom = snapshot.bom();
        Optional<Material> materialOpt = bom.findMaterial(targetId);
        if (materialOpt.isEmpty()) {
            return ToolExecutionResult.failure("Material not found in BOM catalog: " + targetId.value());
        }

        Material material = materialOpt.get();
        List<BomEntry> directComponents = bom.getDirectComponents(targetId);
        List<BomEntry> directParents = bom.getDirectParents(targetId);

        List<Map<String, Object>> componentsData = new ArrayList<>();
        for (BomEntry comp : directComponents) {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("componentId", comp.childMaterialId().value());
            cMap.put("quantityPerParent", comp.quantityPerParent().amount().toPlainString());
            cMap.put("scrapRate", comp.scrapRate().toPlainString());
            cMap.put("position", comp.position());
            componentsData.add(cMap);
        }

        List<String> parentIds = directParents.stream().map(p -> p.parentMaterialId().value()).toList();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("materialId", material.getId().value());
        responseData.put("sku", material.getSku());
        responseData.put("materialName", material.getName());
        responseData.put("category", material.getType().name());
        responseData.put("baseUom", material.getBaseUnit().name());
        responseData.put("components", componentsData);
        responseData.put("parentMaterials", parentIds);
        responseData.put("isLeaf", directComponents.isEmpty());

        return ToolExecutionResult.success(responseData);
    }
}
