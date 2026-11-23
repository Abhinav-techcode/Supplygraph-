package com.supplygraph.web.init;

import com.supplygraph.industry.automotive.AutomotiveIndustryPack;
import com.supplygraph.industry.ev.EvBatteryIndustryPack;
import com.supplygraph.industry.pharma.PharmaIndustryPack;
import com.supplygraph.industry.semiconductor.SemiconductorIndustryPack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The four industry modules are on the classpath but are not Spring-scanned; registering them makes the Extension Registry list what is really installed. */
@Configuration
public class IndustryPacksConfiguration {
    @Bean public EvBatteryIndustryPack evBatteryIndustryPack() { return new EvBatteryIndustryPack(); }
    @Bean public PharmaIndustryPack pharmaIndustryPack() { return new PharmaIndustryPack(); }
    @Bean public SemiconductorIndustryPack semiconductorIndustryPack() { return new SemiconductorIndustryPack(); }
    @Bean public AutomotiveIndustryPack automotiveIndustryPack() { return new AutomotiveIndustryPack(); }
}
