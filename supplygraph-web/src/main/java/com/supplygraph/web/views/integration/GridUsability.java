package com.supplygraph.web.views.integration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Puts action columns (buttons) first and frozen so they are reachable on narrow windows without horizontal scrolling. */
public final class GridUsability {
    private static final Set<String> ACTION_HEADERS = Set.of("Action", "Decision", "Resolve", "Human decision");
    private GridUsability() {}

    public static void actionsFirst(Component root) {
        root.getChildren().forEach(GridUsability::actionsFirst);
        if (root instanceof Grid<?> g) reorder(g);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void reorder(Grid grid) {
        List<Grid.Column> cols = new ArrayList<>(grid.getColumns());
        List<Grid.Column> actions = cols.stream().filter(c -> ACTION_HEADERS.contains(c.getHeaderText())).toList();
        if (actions.isEmpty() || cols.get(0) == actions.get(0)) return;
        cols.removeAll(actions);
        cols.addAll(0, actions);
        grid.setColumnOrder(cols);
        actions.forEach(c -> { c.setFrozen(true); c.setAutoWidth(true); c.setFlexGrow(0); });
    }
}
