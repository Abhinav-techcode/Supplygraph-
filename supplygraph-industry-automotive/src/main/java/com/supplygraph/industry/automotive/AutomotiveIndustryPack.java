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
}

