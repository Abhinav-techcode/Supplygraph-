package com.supplygraph.industry.semiconductor;

import com.supplygraph.industry.api.IndustryIdentifier;
import com.supplygraph.industry.api.IndustryPackExtension;

import java.util.List;

/**
 * Industry Intelligence Pack for Semiconductor Manufacturing.
 */
public class SemiconductorIndustryPack implements IndustryPackExtension {

    public static final IndustryIdentifier ID = IndustryIdentifier.of("SEMICONDUCTOR");

    @Override
    public IndustryIdentifier getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Semiconductor Manufacturing";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "Intelligence pack for wafer fab capacity, photolithography tools, specialty chemicals, and yield loss";
    }

    @Override
    public List<String> getSupportedScenarioCodes() {
        return List.of(
                "SC-SC-01-WAFER-SHORTAGE",
                "SC-SC-02-CHEMICAL-SHORTAGE",
                "SC-SC-03-FAB-EQUIPMENT-BREAKDOWN",
                "SC-SC-04-CLEANROOM-ANOMALY",
                "SC-SC-05-YIELD-DETERIORATION",
                "SC-SC-06-PACKAGING-BOTTLENECK",
                "SC-SC-07-DEMAND-SPIKE",
                "SC-SC-08-GEOPOLITICAL-RESTRICTION"
        );
    }

    @Override
    public List<com.supplygraph.industry.api.IndustryPolicy> getIndustryPolicies() {
        return List.of(
                new com.supplygraph.industry.api.IndustryPolicy("SEMI-POL-01", "Photolithography Gas Sourcing", "EUV photoresist and ultra-pure neon must maintain minimum 60-day strategic reserve", true),
                new com.supplygraph.industry.api.IndustryPolicy("SEMI-POL-02", "Fab Cleanroom Particle Control", "Class 1 cleanroom particle excursion suspends wafer lot stepping immediately", true),
                new com.supplygraph.industry.api.IndustryPolicy("SEMI-POL-03", "Die Yield Degradation Ceiling", "Yield fallout exceeding 8% on advanced nodes triggers automated engineering hold", true)
        );
    }

    @Override
    public List<String> getCriticalMaterials() {
        return List.of("300mm Prime Silicon Substrates", "Extreme Ultraviolet (EUV) Photoresist", "Ultra-High Purity Neon Gas", "Copper Electroplating Precursor", "Palladium Leadframes");
    }

    @Override
    public List<String> getQualityValidationRules() {
        return List.of(
                "Wafer bow/warp parameters must conform to SEMI M1 standard",
                "Spectroscopic impurity verification for etching chemicals",
                "EUV tool uptime and calibration drift logging"
        );
    }

    @Override
    public boolean isMaterialCompatible(String materialName, String contextDescription) {
        if (materialName == null) return false;
        if (contextDescription != null && contextDescription.toLowerCase().contains("300mm")) {
            return !materialName.contains("200mm");
        }
        return true;
    }

    @Override
    public String evaluateSupplierQualification(String supplierName, String country) {
        if (supplierName != null && supplierName.toLowerCase().contains("unvalidated_fab")) {
            return "NON_QUALIFIED_FOUNDRY";
        }
        return "QUALIFIED";
    }
}

