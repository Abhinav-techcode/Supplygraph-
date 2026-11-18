package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.sensitivity.SensitivityAnalysisReport;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for sensitivity and robustness analysis reports.
 */
public interface SensitivityAnalysisRepositoryPort {

    void save(SensitivityAnalysisReport report);

    Optional<SensitivityAnalysisReport> findById(String reportId);

    List<SensitivityAnalysisReport> findByWorkspaceId(String workspaceId);

    List<SensitivityAnalysisReport> findAll();
}

