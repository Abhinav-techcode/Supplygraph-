package com.supplygraph.domain.model.collaboration;

public enum DocumentCategory {
    QUALITY_CERTIFICATE("ISO / IATF Certificate of Analysis (CoA)"),
    SAFETY_DATA_SHEET("Material Safety Data Sheet (MSDS)"),
    TECHNICAL_DRAWING("CAD 2D/3D Technical Specification"),
    MASTER_AGREEMENT("Master Sourcing Agreement / SLA"),
    ESG_EVIDENCE("Carbon Footprint & CSRD Audit Evidence");

    private final String description;

    DocumentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

