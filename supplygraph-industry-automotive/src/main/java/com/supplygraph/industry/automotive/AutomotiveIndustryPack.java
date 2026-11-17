package com.supplygraph.industry.automotive;

import com.supplygraph.industry.api.IndustryIdentifier;
import com.supplygraph.industry.api.IndustryPackExtension;

import java.util.List;

/**
 * Industry Intelligence Pack for Automotive Component Manufacturing.
 */
public class AutomotiveIndustryPack implements IndustryPackExtension {

    public static final IndustryIdentifier ID = IndustryIdentifier.of("AUTOMOTIVE");

    @Override
    public IndustryIdentifier getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Automotive Component Manufacturing";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "Intelligence pack for JIT/JIS delivery, line-stop risks, Tier-2/3 hidden dependencies, and stamping bottlenecks";
    }

    @Override
    public List<String> getSupportedScenarioCodes() {
        return List.of(
                "AU-SC-01-CRITICAL-PART-SHORTAGE",
                "AU-SC-02-TIER2-TIER3-FAILURE",
                "AU-SC-03-JIT-DELIVERY-FAILURE",
                "AU-SC-04-ASSEMBLY-LINE-BREAKDOWN",
                "AU-SC-05-QUALITY-DEFECT",
                "AU-SC-06-TRANSPORT-DISRUPTION",
                "AU-SC-07-DEMAND-SURGE",
                "AU-SC-08-SUPPLIER-CAPACITY-REDUCTION"
        );
    }

    @Override
    public List<com.supplygraph.industry.api.IndustryPolicy> getIndustryPolicies() {
        return List.of(
                new com.supplygraph.industry.api.IndustryPolicy("AUTO-POL-01", "Just-In-Time Buffer Limit", "Assembly plant dock buffer must maintain minimum 4-hour sequenced staging", true),
                new com.supplygraph.industry.api.IndustryPolicy("AUTO-POL-02", "Automotive Grade PPAP Validation", "Production Part Approval Process (PPAP Level 3) required for any alternate supplier switch", true),
                new com.supplygraph.industry.api.IndustryPolicy("AUTO-POL-03", "Electronic Stability Wiring Harness Single-Sourcing", "Critical wiring harnesses must not exceed single-supplier concentration threshold of 75%", true)
        );
    }

    @Override
    public List<String> getCriticalMaterials() {
        return List.of("Automotive Grade 32-bit MCU", "High-Voltage Wiring Harness Harness-A", "Dual-Clutch Transmission Valve Body", "Lightweight Aluminum Sheet Alloy 6016", "Brembo Brake Caliper Assembly");
    }

    @Override
    public List<String> getQualityValidationRules() {
        return List.of(
                "IATF 16949 certification status verification for Tier-1/2 suppliers",
                "Just-in-sequence (JIS) serial code synchronization check",
                "Dimensional tolerance Cpk >= 1.67 compliance audit"
        );
    }

    @Override
    public boolean isMaterialCompatible(String materialName, String contextDescription) {
        if (materialName == null) return false;
        if (contextDescription != null && contextDescription.toLowerCase().contains("ev_chassis")) {
            return !materialName.toLowerCase().contains("diesel_exhaust");
        }
        return true;
    }

    @Override
    public String evaluateSupplierQualification(String supplierName, String country) {
        if (supplierName != null && supplierName.toLowerCase().contains("unaccredited")) {
            return "PPAP_REJECTED";
        }
        return "QUALIFIED";
    }
}

