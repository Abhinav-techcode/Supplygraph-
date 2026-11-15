package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.UnitOfMeasure;

/**
 * End-item finished product delivered to customers.
 */
public class FinishedProduct extends Material {
    private final String productLine;
    private final int warrantyMonths;

    public FinishedProduct(MaterialId id, String sku, String name, UnitOfMeasure baseUnit,
                           String productLine, int warrantyMonths) {
        super(id, sku, name, MaterialType.FINISHED_PRODUCT, baseUnit);
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("Warranty months cannot be negative");
        }
        this.productLine = productLine;
        this.warrantyMonths = warrantyMonths;
    }

    public String getProductLine() {
        return productLine;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }
}

