package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.UnitOfMeasure;

/**
 * Component material purchased or manufactured for assembly.
 */
public class Component extends Material {
    private final int procurementLeadTimeDays;

    public Component(MaterialId id, String sku, String name, UnitOfMeasure baseUnit, int procurementLeadTimeDays) {
        super(id, sku, name, MaterialType.COMPONENT, baseUnit);
        if (procurementLeadTimeDays < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative");
        }
        this.procurementLeadTimeDays = procurementLeadTimeDays;
    }

    public int getProcurementLeadTimeDays() {
        return procurementLeadTimeDays;
    }
}

