package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.Quantity;

import java.math.BigDecimal;
import java.util.*;

/**
 * Directed Acyclic Graph (DAG) representation of a multi-tier Bill of Materials.
 * Enforces cycle detection, deterministic topological ordering, and scrap-adjusted explosion.
 */
public class BillOfMaterials {
    private final Map<MaterialId, Material> materials;
    private final List<BomEntry> entries;
    private final Map<MaterialId, List<BomEntry>> childrenByParent;
    private final Map<MaterialId, List<BomEntry>> parentsByChild;

    public BillOfMaterials(Collection<Material> materialCatalog, Collection<BomEntry> bomEntries) {
        Objects.requireNonNull(materialCatalog, "Material catalog must not be null");
        Objects.requireNonNull(bomEntries, "BOM entries must not be null");

        Map<MaterialId, Material> matMap = new LinkedHashMap<>();
        for (Material m : materialCatalog) {
            matMap.put(m.getId(), m);
        }
        this.materials = Collections.unmodifiableMap(matMap);

        Map<MaterialId, List<BomEntry>> childrenMap = new LinkedHashMap<>();
        Map<MaterialId, List<BomEntry>> parentsMap = new LinkedHashMap<>();
        List<BomEntry> entryList = new ArrayList<>();

        for (BomEntry entry : bomEntries) {
            if (!matMap.containsKey(entry.parentMaterialId())) {
                throw new IllegalArgumentException("Parent material not found in catalog: " + entry.parentMaterialId());
            }
            if (!matMap.containsKey(entry.childMaterialId())) {
                throw new IllegalArgumentException("Child material not found in catalog: " + entry.childMaterialId());
            }
            entryList.add(entry);
            childrenMap.computeIfAbsent(entry.parentMaterialId(), k -> new ArrayList<>()).add(entry);
            parentsMap.computeIfAbsent(entry.childMaterialId(), k -> new ArrayList<>()).add(entry);
        }

        this.entries = Collections.unmodifiableList(entryList);
        this.childrenByParent = Collections.unmodifiableMap(childrenMap);
        this.parentsByChild = Collections.unmodifiableMap(parentsMap);

        // Enforce DAG: Cycle detection on construction
        validateNoCycles(this.materials.keySet(), this.childrenByParent);
    }

    private static void validateNoCycles(Set<MaterialId> allMaterials, Map<MaterialId, List<BomEntry>> childrenByParent) {
        Map<MaterialId, Integer> state = new HashMap<>();
        List<MaterialId> path = new ArrayList<>();

        for (MaterialId node : allMaterials) {
            if (state.getOrDefault(node, 0) == 0) {
                dfsCycleCheck(node, childrenByParent, state, path);
            }
        }
    }

    private static void dfsCycleCheck(MaterialId current, Map<MaterialId, List<BomEntry>> childrenByParent,
                                      Map<MaterialId, Integer> state, List<MaterialId> path) {
        state.put(current, 1); // visiting
        path.add(current);

        List<BomEntry> children = childrenByParent.getOrDefault(current, List.of());
        for (BomEntry edge : children) {
            MaterialId next = edge.childMaterialId();
            Integer nextState = state.getOrDefault(next, 0);
            if (nextState == 1) { // back edge detected -> cycle
                int cycleStartIndex = path.indexOf(next);
                List<MaterialId> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
                cycle.add(next);
                throw new IllegalStateException("Cycle detected in Bill of Materials: " + cycle);
            } else if (nextState == 0) {
                dfsCycleCheck(next, childrenByParent, state, path);
            }
        }

        path.remove(path.size() - 1);
        state.put(current, 2); // visited
    }

    public static BillOfMaterials empty() {
        return new BillOfMaterials(List.of(), List.of());
    }

    public Optional<Material> findMaterial(MaterialId id) {
        return Optional.ofNullable(materials.get(id));
    }

    public Material getMaterial(MaterialId id) {
        Material mat = materials.get(id);
        if (mat == null) {
            throw new NoSuchElementException("Material not found: " + id);
        }
        return mat;
    }

    public List<BomEntry> getDirectComponents(MaterialId parentId) {
        return childrenByParent.getOrDefault(parentId, List.of());
    }

    public List<BomEntry> getDirectParents(MaterialId childId) {
        return parentsByChild.getOrDefault(childId, List.of());
    }

    public List<BomEntry> getAllEntries() {
        return entries;
    }

    public Collection<Material> getAllMaterials() {
        return materials.values();
    }

    public int getMaterialCount() {
        return materials.size();
    }

    /**
     * Deterministic topological ordering of materials from leaf components to top-level products.
     */
    public List<MaterialId> getTopologicalOrder() {
        Map<MaterialId, Integer> inDegree = new LinkedHashMap<>();
        for (MaterialId id : materials.keySet()) {
            inDegree.put(id, 0);
        }

        for (BomEntry entry : entries) {
            inDegree.put(entry.parentMaterialId(), inDegree.get(entry.parentMaterialId()) + 1);
        }

        Queue<MaterialId> queue = new ArrayDeque<>();
        for (Map.Entry<MaterialId, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<MaterialId> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            MaterialId leaf = queue.poll();
            ordered.add(leaf);

            for (BomEntry parentEdge : getDirectParents(leaf)) {
                MaterialId parentId = parentEdge.parentMaterialId();
                int updated = inDegree.get(parentId) - 1;
                inDegree.put(parentId, updated);
                if (updated == 0) {
                    queue.add(parentId);
                }
            }
        }

        if (ordered.size() != materials.size()) {
            throw new IllegalStateException("Topological sort failed: graph contains unresolvable cycles");
        }

        return Collections.unmodifiableList(ordered);
    }

    /**
     * Multi-level BOM explosion: computes the exact gross material requirements across all sub-tiers,
     * including scrap rate adjustments and unit propagation.
     */
    public Map<MaterialId, Quantity> explodeRequirements(MaterialId rootProductId, Quantity rootQuantity) {
        Objects.requireNonNull(rootProductId, "Root product ID must not be null");
        Objects.requireNonNull(rootQuantity, "Root quantity must not be null");
        if (!materials.containsKey(rootProductId)) {
            throw new NoSuchElementException("Root product not in BOM catalog: " + rootProductId);
        }

        Map<MaterialId, Quantity> aggregatedRequirements = new LinkedHashMap<>();
        Queue<RequirementStep> queue = new ArrayDeque<>();
        queue.add(new RequirementStep(rootProductId, rootQuantity));

        while (!queue.isEmpty()) {
            RequirementStep step = queue.poll();
            List<BomEntry> children = getDirectComponents(step.materialId());

            for (BomEntry entry : children) {
                Quantity childGross = entry.calculateGrossRequired(step.quantity());
                MaterialId childId = entry.childMaterialId();

                aggregatedRequirements.merge(childId, childGross, Quantity::add);

                // Continue recursion if child itself has sub-components
                if (!getDirectComponents(childId).isEmpty()) {
                    queue.add(new RequirementStep(childId, childGross));
                }
            }
        }

        return Collections.unmodifiableMap(aggregatedRequirements);
    }

    private record RequirementStep(MaterialId materialId, Quantity quantity) {}
}
