package com.supplygraph.application.tool.builtin;

import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.domain.model.capacity.CapacityCalendar;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Read-only tool allowing bounded agents to query resource capacity windows and reservations.
 */
public class QueryCapacityTool implements AgentTool {

    public static final String NAME = "query_capacity";

    private final Supplier<Map<ResourceId, CapacityCalendar>> calendarsSupplier;

    public QueryCapacityTool(Supplier<Map<ResourceId, CapacityCalendar>> calendarsSupplier) {
        this.calendarsSupplier = Objects.requireNonNull(calendarsSupplier, "calendarsSupplier must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Query manufacturing and logistics resource capacity, existing reservations, and available capacity windows.";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        Object resIdObj = request.parameters().get("resourceId");
        if (resIdObj == null || resIdObj.toString().isBlank()) {
            return ToolExecutionResult.failure("Missing required parameter: resourceId");
        }
        ResourceId targetResource = ResourceId.of(resIdObj.toString());

        Map<ResourceId, CapacityCalendar> calendars = calendarsSupplier.get();
        if (calendars == null || !calendars.containsKey(targetResource)) {
            return ToolExecutionResult.failure("Resource not found or has no active capacity calendar: " + targetResource.value());
        }

        CapacityCalendar calendar = calendars.get(targetResource);

        Instant start = request.parameters().get("start") != null
                ? Instant.parse(request.parameters().get("start").toString())
                : Instant.now();
        Instant end = request.parameters().get("end") != null
                ? Instant.parse(request.parameters().get("end").toString())
                : start.plusSeconds(86400 * 7); // Default 7-day query window

        CapacityWindow queryWindow = CapacityWindow.of(start, end);
        Quantity reserved = calendar.getReservedQuantity(queryWindow);
        Quantity available = calendar.getAvailableCapacity(queryWindow);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("resourceId", targetResource.value());
        responseData.put("ratePerHour", calendar.getCapacity().ratePerHour().amount().toPlainString());
        responseData.put("unitOfMeasure", calendar.getCapacity().ratePerHour().unit().name());
        responseData.put("queryWindowStart", start.toString());
        responseData.put("queryWindowEnd", end.toString());
        responseData.put("reservedCapacity", reserved.amount().toPlainString());
        responseData.put("availableCapacity", available.amount().toPlainString());
        responseData.put("reservationCount", calendar.findOverlappingReservations(queryWindow).size());

        return ToolExecutionResult.success(responseData);
    }
}
