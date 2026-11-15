package com.supplygraph.industry.ev;

import com.supplygraph.industry.api.IndustryIdentifier;
import com.supplygraph.industry.api.IndustryPackExtension;

import java.util.List;

/**
 * Industry Intelligence Pack for EV Battery Manufacturing.
 */
public class EvBatteryIndustryPack implements IndustryPackExtension {

    public static final IndustryIdentifier ID = IndustryIdentifier.of("EV_BATTERY");

    @Override
    public IndustryIdentifier getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "EV Battery Manufacturing";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "Intelligence pack for EV battery cell chemistry, precursor shortages, and dry-room constraints";
    }

    @Override
    public List<String> getSupportedScenarioCodes() {
        return List.of(
                "EV-SC-01-LITHIUM-SHORTAGE",
                "EV-SC-02-NICKEL-PRICE-SURGE",
                "EV-SC-03-SHIPMENT-DELAY",
                "EV-SC-04-TEMP-EXCURSION",
                "EV-SC-05-EQUIPMENT-FAILURE",
                "EV-SC-06-QUALITY-REJECTION",
                "EV-SC-07-DEMAND-SURGE",
                "EV-SC-08-RECYCLING-DISRUPTION"
        );
    }
}

