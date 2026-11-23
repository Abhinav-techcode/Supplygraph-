package com.supplygraph.web.views;

import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.status.SystemStatusService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Read-only window onto the database the application is really using: which server, which database, every table with its row count, and the first rows of any table.
 * Table names are only ever taken from the database's own catalogue, and columns that hold secrets are masked.
 */
@Route(value = "database", layout = MainLayout.class)
@PageTitle("Database | SupplyGraph")
public class DatabaseView extends VerticalLayout {
    private static final Pattern SECRET = Pattern.compile("(?i).*(password|hash|salt|secret|token|api_?key|signature|credential).*");
    private static final int PREVIEW_ROWS = 50;

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final Div preview = new Div();
    private final Grid<String[]> tables = new Grid<>();
    private final List<String[]> allTables = new ArrayList<>();

    public DatabaseView(DataSource dataSource, JdbcTemplate jdbc, SystemStatusService status, @Value("${supplygraph.database-browser.enabled:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");
        add(new H2("Database"));
        if (!enabled) {
            add(new Paragraph("The database browser is switched off (supplygraph.database-browser.enabled=false)."));
            return;
        }
        var s = status.current();
        Span badge = new Span(s.connected() ? "Connected" : "Not connected");
        badge.getElement().getThemeList().add(s.connected() ? "badge success" : "badge error");
        add(badge, new Paragraph(s.connected()
                ? s.product() + " at " + s.where() + (s.persistent() ? ". Data is kept between restarts." : ". In-memory: data is lost when the application stops.") + " Schema version " + s.migration() + "."
                : s.problem()));
        if (!s.connected()) return;
        add(new Paragraph("These are the real tables the application reads and writes. Everything here is read-only. To look at the same data with your own tool, connect to the address above "
                + "(for the demo container: host localhost, port 5433, database supplygraph, user postgres)."));

        TextField filter = new TextField();
        filter.setPlaceholder("Filter tables…");
        filter.setClearButtonVisible(true);
        filter.setWidth("260px");
        filter.addValueChangeListener(e -> showTables(e.getValue()));
        Button refresh = new Button("Refresh counts", e -> { loadTables(); showTables(filter.getValue()); });
        refresh.addThemeVariants(ButtonVariant.LUMO_SMALL);

        tables.addColumn(r -> r[0]).setHeader("Table").setFlexGrow(3).setSortable(true);
        tables.addColumn(r -> r[1]).setHeader("Rows").setComparator(Comparator.comparingLong(r -> Long.parseLong(r[1]))).setSortable(true).setFlexGrow(1);
        tables.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        tables.setHeight("340px");
        tables.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(r -> showRows(r[0])));
        loadTables();
        showTables("");
        preview.setWidthFull();
        add(new HorizontalLayout(filter, refresh), new Span(allTables.size() + " tables · " + allTables.stream().mapToLong(r -> Long.parseLong(r[1])).sum() + " rows in total. Select a table to see its first " + PREVIEW_ROWS + " rows."),
                tables, preview);
    }

    private void loadTables() {
        allTables.clear();
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            List<String> names = new ArrayList<>();
            try (ResultSet rs = md.getTables(c.getCatalog(), c.getSchema(), "%", new String[]{"TABLE"})) {
                while (rs.next()) names.add(rs.getString("TABLE_NAME"));
            }
            Collections.sort(names);
            for (String n : names) allTables.add(new String[]{n, String.valueOf(count(n))});
        } catch (Exception e) {
            preview.removeAll();
            preview.add(new Paragraph("Could not read the table list: " + e.getMessage()));
        }
    }

    private long count(String table) {
        try {
            Long n = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + table.replace("\"", "") + "\"", Long.class);
            return n == null ? 0 : n;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private void showTables(String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        tables.setItems(allTables.stream().filter(r -> r[0].toLowerCase(Locale.ROOT).contains(f)).toList());
    }

    private void showRows(String table) {
        preview.removeAll();
        if (allTables.stream().noneMatch(r -> r[0].equals(table))) return;   // only names that came from the catalogue are ever used in SQL
        preview.add(new H3(table));
        try (Connection c = dataSource.getConnection(); var st = c.createStatement()) {
            st.setMaxRows(PREVIEW_ROWS);
            try (ResultSet rs = st.executeQuery("SELECT * FROM \"" + table.replace("\"", "") + "\"")) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                Grid<String[]> grid = new Grid<>();
                grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
                for (int i = 0; i < cols; i++) {
                    final int idx = i;
                    String col = md.getColumnLabel(i + 1);
                    grid.addColumn(r -> r[idx]).setHeader(col).setAutoWidth(true).setResizable(true);
                }
                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    String[] r = new String[cols];
                    for (int i = 0; i < cols; i++) {
                        String name = md.getColumnLabel(i + 1);
                        Object v = rs.getObject(i + 1);
                        String text = v == null ? "" : String.valueOf(v);
                        r[i] = SECRET.matcher(name).matches() ? "••••••" : text.length() > 120 ? text.substring(0, 119) + "…" : text;
                    }
                    rows.add(r);
                }
                grid.setItems(rows);
                grid.setHeight("360px");
                preview.add(new Paragraph(rows.isEmpty() ? "This table is empty." : "Showing " + rows.size() + " row(s). Columns that hold secrets are masked."), grid);
            }
        } catch (Exception e) {
            preview.add(new Paragraph("Could not read this table: " + e.getMessage()));
        }
    }
}
