package com.supplygraph.domain.model.planning;

/**
 * Multi-echelon supply network supply tiers for echelon inventory positioning.
 */
public enum EchelonTier {
    CENTRAL_DC("Central Distribution Centre", 1),
    REGIONAL_HUB("Regional Fulfillment Hub", 2),
    PLANT_WAREHOUSE("Plant Finished Goods Warehouse", 3),
    RETAIL_STORE("Point of Sale / Retail Node", 4);

    private final String description;
    private final int level;

    EchelonTier(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }
}

