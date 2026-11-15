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
}

