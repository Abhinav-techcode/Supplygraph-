package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.quality.DataQualityReport;
import com.supplygraph.domain.model.quality.DataQualityReportId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for automated data quality assessment reports and findings.
 */
public interface DataQualityRepositoryPort {

    void save(DataQualityReport report);

    Optional<DataQualityReport> findById(DataQualityReportId id);

    Optional<DataQualityReport> findLatestByWorkspaceId(String workspaceId);

    List<DataQualityReport> findHistoryByWorkspaceId(String workspaceId);
}
