package com.supplygraph.web.components;

import com.supplygraph.application.graph.NetworkGraphView;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Network map component (pure Java)")
class DigitalTwinNetworkGraphComponentTest {

    private static NetworkGraphView view() {
        var a = new NetworkGraphView.Node("SUP-A", "Supplier A", "Oslo", NodeType.SUPPLIER, "Tier 1", 0, 0, true, true, List.of("Fire"), 0, "n/a");
        var h = new NetworkGraphView.Node("HUB", "Hub", "Hamburg", NodeType.LOGISTICS_HUB, "Hub", 1, 0, true, false, List.of(), 500, "500 pallets/day");
        var p = new NetworkGraphView.Node("PLANT", "Plant", "Munich", NodeType.MANUFACTURING_SITE, "Plant", 2, 0, true, false, List.of(), 900, "40 units/h");
        return new NetworkGraphView(WorkspaceId.of("WS"), 3, List.of(a, h, p),
                List.of(new NetworkGraphView.Lane("L1", "SUP-A", "HUB", "ROAD", 24, true, true), new NetworkGraphView.Lane("L2", "HUB", "PLANT", "RAIL", 12, true, false)),
                List.of(new NetworkGraphView.Layer(0, "a"), new NetworkGraphView.Layer(1, "b"), new NetworkGraphView.Layer(2, "c")),
                List.of(), Instant.now());
    }

    private static List<com.vaadin.flow.component.Component> descendants(com.vaadin.flow.component.Component c) {
        return c.getChildren().flatMap(x -> java.util.stream.Stream.concat(java.util.stream.Stream.of(x), descendants(x).stream())).collect(Collectors.toList());
    }

    private static Set<String> classesOf(DigitalTwinNetworkGraphComponent g, String idPrefix) {
        return descendants(g).stream().filter(c -> c.getElement().getClassList().contains("sg-gnode")
                && c.getChildren().anyMatch(ch -> ch.getElement().getText().startsWith(idPrefix))).findFirst().orElseThrow()
                .getElement().getClassList().stream().collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Facilities are real components; the disrupted one is marked and pulses")
    void rendersNodesFromTheView() {
        var g = new DigitalTwinNetworkGraphComponent(view());
        assertThat(descendants(g).stream().filter(c -> c.getElement().getClassList().contains("sg-gnode"))).hasSize(3);
        assertThat(classesOf(g, "SUP-A")).contains("sg-disrupted");
        assertThat(classesOf(g, "PLANT")).doesNotContain("sg-disrupted");
    }

    @Test
    @DisplayName("Overlay marks impacted, route and agent-target facilities")
    void overlayIsApplied() {
        var g = new DigitalTwinNetworkGraphComponent(view());
        g.setOverlay(new DigitalTwinNetworkGraphComponent.Overlay(Set.of("HUB", "PLANT"), List.of("HUB", "PLANT"), Set.of("PLANT")));
        assertThat(classesOf(g, "HUB")).contains("sg-impacted", "sg-route");
        assertThat(classesOf(g, "PLANT")).contains("sg-marked", "sg-route");
    }

    @Test
    @DisplayName("Selecting a facility traces its supply path and dims unrelated ones")
    void selectionTracesThePath() {
        var g = new DigitalTwinNetworkGraphComponent(view());
        assertThat(classesOf(g, "PLANT")).doesNotContain("sg-dim");   // first view shows the whole network
        g.selectNodeById("HUB");
        assertThat(classesOf(g, "HUB")).contains("sg-selected");
        assertThat(classesOf(g, "SUP-A")).doesNotContain("sg-dim");   // upstream of HUB
        assertThat(classesOf(g, "PLANT")).doesNotContain("sg-dim");   // downstream of HUB
    }

    @Test
    @DisplayName("An empty twin shows an explicit empty state instead of sample data")
    void emptyState() {
        var empty = new NetworkGraphView(WorkspaceId.of("WS"), 1, List.of(), List.of(), List.of(), List.of(), Instant.now());
        var g = new DigitalTwinNetworkGraphComponent(empty);
        assertThat(descendants(g).stream().anyMatch(c -> c.getElement().getClassList().contains("sg-empty"))).isTrue();
        assertThat(descendants(g).stream().noneMatch(c -> c.getElement().getClassList().contains("sg-gnode"))).isTrue();
    }
}
