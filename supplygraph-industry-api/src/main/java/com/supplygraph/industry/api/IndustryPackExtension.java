package com.supplygraph.industry.api;

import java.util.List;

/**
 * Service Provider Interface (SPI) for pluggable Industry Intelligence Packs.
 * Adding an industry pack implements this interface without altering shared core modules.
 */
public interface IndustryPackExtension {

    IndustryIdentifier getId();

    String getDisplayName();

    String getVersion();

    String getDescription();

    List<String> getSupportedScenarioCodes();

    default List<IndustryPolicy> getIndustryPolicies() {
        return List.of();
    }

    default List<String> getCriticalMaterials() {
        return List.of();
    }

    default List<String> getQualityValidationRules() {
        return List.of();
    }

    default boolean isMaterialCompatible(String materialName, String contextDescription) {
        return true;
    }

    default String evaluateSupplierQualification(String supplierName, String country) {
        return "QUALIFIED";
    }
}

