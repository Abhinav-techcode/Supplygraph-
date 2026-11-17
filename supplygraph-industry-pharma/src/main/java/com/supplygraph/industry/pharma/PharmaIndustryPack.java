package com.supplygraph.industry.pharma;

import com.supplygraph.industry.api.IndustryIdentifier;
import com.supplygraph.industry.api.IndustryPackExtension;

import java.util.List;

/**
 * Industry Intelligence Pack for Pharmaceutical Supply Chain.
 */
public class PharmaIndustryPack implements IndustryPackExtension {

    public static final IndustryIdentifier ID = IndustryIdentifier.of("PHARMACEUTICAL");

    @Override
    public IndustryIdentifier getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Pharmaceutical Supply Chain";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "Intelligence pack for API shortages, cold-chain integrity, GMP regulations, and batch holds";
    }

    @Override
    public List<String> getSupportedScenarioCodes() {
        return List.of(
                "PH-SC-01-API-SHORTAGE",
                "PH-SC-02-COLD-CHAIN-EXCURSION",
                "PH-SC-03-EXPIRY-RISK",
                "PH-SC-04-BATCH-FAILURE",
                "PH-SC-05-PACKAGING-SHORTAGE",
                "PH-SC-06-REGULATORY-HOLD",
                "PH-SC-07-DEMAND-SURGE",
                "PH-SC-08-WAREHOUSE-DISRUPTION"
        );
    }

    @Override
    public List<com.supplygraph.industry.api.IndustryPolicy> getIndustryPolicies() {
        return List.of(
                new com.supplygraph.industry.api.IndustryPolicy("PHA-POL-01", "Cold-Chain Integrity SLA", "Biologics and sterile APIs must maintain 2C-8C continuously; temperature excursion prompts quarantine", true),
                new com.supplygraph.industry.api.IndustryPolicy("PHA-POL-02", "GMP Batch Traceability", "Active Pharmaceutical Ingredients must have certificate of analysis and full genealogy", true),
                new com.supplygraph.industry.api.IndustryPolicy("PHA-POL-03", "Shelf-Life Buffer Threshold", "Materials with less than 90 days remaining shelf-life cannot be issued to formulation lines", true)
        );
    }

    @Override
    public List<String> getCriticalMaterials() {
        return List.of("Semaglutide API", "Sterile Water for Injection", "Type-I Borosilicate Glass Vials", "Fluoropolymer Elastomeric Stoppers", "Monoclonal Antibody Bulk");
    }

    @Override
    public List<String> getQualityValidationRules() {
        return List.of(
                "Lot expiration date must be in the future (minimum 180 days for raw materials)",
                "Quality hold flag requires verified QA sign-off prior to reservation",
                "Batch Certificate of Analysis (CoA) document hash validation"
        );
    }

    @Override
    public boolean isMaterialCompatible(String materialName, String contextDescription) {
        if (materialName == null) return false;
        if (contextDescription != null && contextDescription.toLowerCase().contains("cold_chain")) {
            return !materialName.toLowerCase().contains("ambient_only");
        }
        return true;
    }

    @Override
    public String evaluateSupplierQualification(String supplierName, String country) {
        if (supplierName != null && supplierName.toLowerCase().contains("uninspected")) {
            return "PROVISIONAL_PENDING_GMP_AUDIT";
        }
        return "QUALIFIED";
    }
}

