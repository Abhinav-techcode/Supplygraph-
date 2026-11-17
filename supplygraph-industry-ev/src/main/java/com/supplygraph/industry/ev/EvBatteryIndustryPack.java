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

    @Override
    public List<com.supplygraph.industry.api.IndustryPolicy> getIndustryPolicies() {
        return List.of(
                new com.supplygraph.industry.api.IndustryPolicy("EV-POL-01", "Cathode Chemistry Segregation", "NMC811 and LFP precursors must not share unpurged mixing hoppers", true),
                new com.supplygraph.industry.api.IndustryPolicy("EV-POL-02", "Responsible Mineral Sourcing", "Cobalt and Lithium must maintain certified conflict-free chain-of-custody", true),
                new com.supplygraph.industry.api.IndustryPolicy("EV-POL-03", "Dry-Room Dew Point Threshold", "Electrolyte filling requires dew point below -40C; violations require quarantine", true)
        );
    }

    @Override
    public List<String> getCriticalMaterials() {
        return List.of("Battery-Grade Lithium Hydroxide", "Class-1 Nickel Sulfate", "Cobalt Sulfate", "Coated Synthetic Graphite", "NMC811 Cathode Active Material");
    }

    @Override
    public List<String> getQualityValidationRules() {
        return List.of(
                "Moisture content in cathode active material must not exceed 200 ppm",
                "Electrolyte lot purity certification required prior to cell assembly",
                "Anode-to-cathode capacity ratio balance verification"
        );
    }

    @Override
    public boolean isMaterialCompatible(String materialName, String contextDescription) {
        if (materialName == null) return false;
        String m = materialName.toLowerCase();
        if (contextDescription != null && contextDescription.toLowerCase().contains("lfp")) {
            return !m.contains("nickel") && !m.contains("cobalt");
        }
        return true;
    }

    @Override
    public String evaluateSupplierQualification(String supplierName, String country) {
        if (country != null && (country.equalsIgnoreCase("PRK") || country.equalsIgnoreCase("IRN"))) {
            return "DISQUALIFIED_SANCTIONED_JURISDICTION";
        }
        return "QUALIFIED";
    }
}

