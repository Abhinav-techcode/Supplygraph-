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
}

