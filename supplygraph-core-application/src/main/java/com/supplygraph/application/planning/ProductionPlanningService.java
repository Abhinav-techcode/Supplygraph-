package com.supplygraph.application.planning;

import com.supplygraph.application.planning.port.out.ProductionScheduleRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ProductionScheduleId;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProductionPlanningService {

    private final ProductionScheduleRepositoryPort scheduleRepository;

    public ProductionPlanningService(ProductionScheduleRepositoryPort scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public FiniteProductionSchedule generateFiniteSchedule(
            WorkspaceId workspaceId,
            NodeId plantId,
            String scheduleName,
            List<ScheduledProductionJob> jobs,
            double standardShiftHoursAvailable
    ) {
        List<ScheduledProductionJob> sortedJobs = jobs.stream()
                .sorted(Comparator.comparing(ScheduledProductionJob::scheduledDate)
                        .thenComparingInt(ScheduledProductionJob::shiftNumber)
                        .thenComparingInt(ScheduledProductionJob::sequenceNumber))
                .toList();

        double totalOccupiedHours = sortedJobs.stream()
                .mapToDouble(ScheduledProductionJob::getTotalOccupiedHours)
                .sum();

        double availableHours = standardShiftHoursAvailable > 0 ? standardShiftHoursAvailable : (sortedJobs.size() * 8.0);
        double utilization = availableHours > 0 ? Math.min(1.0, totalOccupiedHours / availableHours) : 0.0;

        FiniteProductionSchedule schedule = new FiniteProductionSchedule(
                ProductionScheduleId.generate(),
                workspaceId,
                plantId,
                scheduleName,
                sortedJobs,
                utilization,
                Instant.now()
        );

        scheduleRepository.save(schedule);
        return schedule;
    }

    public Optional<FiniteProductionSchedule> getSchedule(ProductionScheduleId id) {
        return scheduleRepository.findById(id);
    }

    public List<FiniteProductionSchedule> listSchedules(WorkspaceId workspaceId) {
        return scheduleRepository.findByWorkspaceId(workspaceId);
    }
}

