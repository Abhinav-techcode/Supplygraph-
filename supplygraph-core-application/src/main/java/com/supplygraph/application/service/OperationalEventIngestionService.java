package com.supplygraph.application.service;

import com.supplygraph.application.port.out.OperationalEventRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service orchestrating validation, deduplication, provenance tracking,
 * and ingestion of operational events from internal and external sources.
 */
@Service
public class OperationalEventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(OperationalEventIngestionService.class);
    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(15);
    private static final Duration MAX_HISTORICAL_AGE = Duration.ofDays(365);

    private final OperationalEventRepositoryPort eventRepository;
    private final OperationalMonitoringService monitoringService;

    public OperationalEventIngestionService(
            OperationalEventRepositoryPort eventRepository,
            OperationalMonitoringService monitoringService
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "OperationalEventRepositoryPort must not be null");
        this.monitoringService = Objects.requireNonNull(monitoringService, "OperationalMonitoringService must not be null");
    }

    @Transactional
    public OperationalEvent ingestEvent(OperationalEvent event) {
        Objects.requireNonNull(event, "OperationalEvent must not be null");

        // 1. Idempotency Check & Deduplication
        Optional<OperationalEvent> existing = eventRepository.findByIdempotencyKey(event.getIdempotencyKey());
        if (existing.isPresent()) {
            log.warn("Duplicate operational event detected with idempotency key: {}", event.getIdempotencyKey());
            event.markDuplicate();
            eventRepository.save(event);
            return event;
        }

        // 2. Structural & Field Validation
        if (event.getAffectedEntityId() == null || event.getAffectedEntityId().isBlank()
                || event.getAffectedEntityType() == null || event.getAffectedEntityType().isBlank()
                || event.getWorkspaceId() == null) {
            event.markRejected(EventRejectionReason.MISSING_REQUIRED_FIELDS);
            eventRepository.save(event);
            log.warn("Event {} rejected: missing required fields", event.getEventId().value());
            return event;
        }

        // 3. Monotonic Timestamp & Clock Skew Validation
        Instant now = Instant.now();
        if (event.getEventTimestamp().isAfter(now.plus(MAX_FUTURE_SKEW))
                || event.getEventTimestamp().isBefore(now.minus(MAX_HISTORICAL_AGE))) {
            event.markRejected(EventRejectionReason.STALE_EVENT_TIMESTAMP);
            eventRepository.save(event);
            log.warn("Event {} rejected: invalid timestamp {} against current time {}",
                    event.getEventId().value(), event.getEventTimestamp(), now);
            return event;
        }

        // 4. Mark Validated & Persist
        event.markValidated();
        eventRepository.save(event);

        // 5. Dispatch to Operational Monitoring Engine
        monitoringService.evaluateEvent(event);
        event.markProcessed();
        eventRepository.save(event);

        log.info("Operational event {} of type {} ingested successfully for entity {}",
                event.getEventId().value(), event.getEventType(), event.getAffectedEntityId());
        return event;
    }

    @Transactional
    public List<OperationalEvent> ingestCsv(String csvContent, WorkspaceId workspaceId, String user) {
        Objects.requireNonNull(csvContent, "csvContent must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        List<OperationalEvent> results = new ArrayList<>();
        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length <= 1) {
            return results;
        }

        String batchId = "BATCH-CSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        EventProvenance provenance = EventProvenance.imported("CSV_IMPORT", batchId, user != null ? user : "OPERATOR");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] cols = line.split(",", -1);
            if (cols.length < 4) {
                OperationalEvent malformed = OperationalEvent.createIngested(
                        EventType.INVENTORY_LEVEL_CHANGED,
                        EventSource.CSV_IMPORT,
                        Instant.now(),
                        "UNKNOWN",
                        "UNKNOWN",
                        workspaceId,
                        Map.of("rawLine", line),
                        provenance,
                        "ERR-" + UUID.randomUUID()
                );
                malformed.markRejected(EventRejectionReason.MALFORMED_PAYLOAD);
                eventRepository.save(malformed);
                results.add(malformed);
                continue;
            }

            try {
                EventType type = EventType.valueOf(cols[0].trim());
                String entityId = cols[1].trim();
                String entityType = cols[2].trim();
                String idempKey = cols[3].trim().isEmpty() ? ("CSV-" + UUID.randomUUID()) : cols[3].trim();

                Map<String, String> payload = new HashMap<>();
                if (cols.length >= 5 && !cols[4].trim().isEmpty()) {
                    String[] pairs = cols[4].trim().split(";");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) {
                            payload.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                }

                OperationalEvent event = OperationalEvent.createIngested(
                        type,
                        EventSource.CSV_IMPORT,
                        Instant.now(),
                        entityId,
                        entityType,
                        workspaceId,
                        payload,
                        provenance,
                        idempKey
                );
                results.add(ingestEvent(event));
            } catch (Exception ex) {
                log.error("Failed to parse CSV line {}: {}", i, line, ex);
                OperationalEvent errorEvent = OperationalEvent.createIngested(
                        EventType.INVENTORY_LEVEL_CHANGED,
                        EventSource.CSV_IMPORT,
                        Instant.now(),
                        "MALFORMED_ROW_" + i,
                        "ROW",
                        workspaceId,
                        Map.of("error", ex.getMessage()),
                        provenance,
                        "ERR-" + UUID.randomUUID()
                );
                errorEvent.markRejected(EventRejectionReason.MALFORMED_PAYLOAD);
                eventRepository.save(errorEvent);
                results.add(errorEvent);
            }
        }
        return results;
    }

    public List<OperationalEvent> getRecentEvents(int limit) {
        return eventRepository.findRecent(limit);
    }

    public List<OperationalEvent> getWorkspaceEvents(WorkspaceId workspaceId, int limit) {
        return eventRepository.findByWorkspaceId(workspaceId, limit);
    }
}

