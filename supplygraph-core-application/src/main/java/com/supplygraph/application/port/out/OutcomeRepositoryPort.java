package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.ActualMeasurement;
import com.supplygraph.domain.model.execution.ReassessmentRequest;

import java.util.List;
import java.util.Optional;

public interface OutcomeRepositoryPort {
    ActualMeasurement saveActual(ActualMeasurement actual);
    List<ActualMeasurement> findActuals(String workflowId);
    ReassessmentRequest saveReassessment(ReassessmentRequest request);
    Optional<ReassessmentRequest> findReassessment(String id);
    List<ReassessmentRequest> findReassessmentsByWorkflow(String workflowId);
    List<ReassessmentRequest> findReassessmentsByWorkspace(WorkspaceId ws, int limit);
}
