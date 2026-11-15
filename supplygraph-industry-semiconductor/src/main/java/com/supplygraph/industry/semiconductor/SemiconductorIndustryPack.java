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
}

