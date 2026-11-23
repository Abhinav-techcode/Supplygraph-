package com.supplygraph.application.service;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DisruptionApplicationServiceTest {

    private DisruptionRepositoryPort disruptionRepo;
    private DigitalTwinRepositoryPort twinRepo;
    private DisruptionApplicationService service;

    private final WorkspaceId ws = WorkspaceId.of("ws-disrupt-test");
    private final NodeId nodeId = NodeId.of("FAB-1");
    private final Map<DisruptionId, Disruption> storage = new HashMap<>();

    @BeforeEach
    void setUp() {
        disruptionRepo = mock(DisruptionRepositoryPort.class);
        twinRepo = mock(DigitalTwinRepositoryPort.class);
        storage.clear();

        doAnswer(invocation -> {
            Disruption d = invocation.getArgument(0);
            storage.put(d.id(), d);
            return null;
        }).when(disruptionRepo).save(any(Disruption.class));

        doAnswer(invocation -> {
            DisruptionId id = invocation.getArgument(0);
            return Optional.ofNullable(storage.get(id));
        }).when(disruptionRepo).findById(any(DisruptionId.class));

        // Create mock digital twin with FAB-1
        SupplyChainNode node = new Supplier(nodeId, "Supplier 1", "Tokyo", true, 1, Set.of(), 0.1);
        SupplyChainGraph graph = new SupplyChainGraph(List.of(node), List.of());
        DigitalTwinState twinState = new DigitalTwinState(ws, StateVersion.of(1L), graph, BillOfMaterials.empty());

        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        service = new DisruptionApplicationService(disruptionRepo, twinRepo);
    }

    @Test
    @DisplayName("Create disruption validates nodes against digital twin and rejects non-existent entities")
    void testCreateDisruptionEntityValidation() {
        Instant now = Instant.now();
        Instant end = now.plusSeconds(3600);

        // Unknown node rejected
        assertThatThrownBy(() -> service.createDisruption(
                ws, "AUTOMOTIVE", "DEFECT", "Defect", "Desc",
                DisruptionCategory.QUALITY_REJECTION, DisruptionSeverity.MEDIUM,
                Set.of(NodeId.of("NON_EXISTENT_NODE")), Set.of(), Set.of(),
                now, end, DisruptionImpactParameters.empty(), "Audit"
        )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown node referenced");

        // Known node succeeds
        Disruption d = service.createDisruption(
                ws, "AUTOMOTIVE", "DEFECT", "Valid Disruption", "Desc",
                DisruptionCategory.QUALITY_REJECTION, DisruptionSeverity.MEDIUM,
                Set.of(nodeId), Set.of(), Set.of(),
                now, end, DisruptionImpactParameters.empty(), "Audit"
        );

        assertThat(d.status()).isEqualTo(DisruptionStatus.DRAFT);
        assertThat(storage).containsKey(d.id());
    }

    @Test
    @DisplayName("Optimistic concurrency conflict throws IllegalStateException when version does not match")
    void testOptimisticConcurrencyConflict() {
        Instant now = Instant.now();
        Disruption d = service.createDisruption(
                ws, "AUTOMOTIVE", "DEFECT", "Title", "Desc",
                DisruptionCategory.QUALITY_REJECTION, DisruptionSeverity.MEDIUM,
                Set.of(nodeId), Set.of(), Set.of(),
                now, now.plusSeconds(3600), DisruptionImpactParameters.empty(), "Audit"
        );

        // Update with correct expectedVersion = 0
        Disruption updated = service.updateDisruption(
                d.id(), 0L, "New Title", "New Desc", DisruptionSeverity.HIGH,
                null, null, null, null, null, null
        );
        assertThat(updated.version()).isEqualTo(1L);

        // Another update attempting with stale expectedVersion = 0 fails
        assertThatThrownBy(() -> service.updateDisruption(
                d.id(), 0L, "Conflict Title", "Desc", DisruptionSeverity.CRITICAL,
                null, null, null, null, null, null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Optimistic concurrency conflict");
    }
}

