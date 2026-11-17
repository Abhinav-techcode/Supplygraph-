package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.domain.model.whatif.WhatIfScenarioId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for isolated What-If scenario evaluations.
 */
public interface WhatIfExperimentRepositoryPort {

    void save(WhatIfComparisonResult result);

    Optional<WhatIfComparisonResult> findByScenarioId(WhatIfScenarioId scenarioId);

    List<WhatIfComparisonResult> findAll();
}
