package com.supplygraph.web.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

/** Answers "is the database there, which one is it, and is the data kept?" without exposing credentials. */
@Service
public class SystemStatusService {
    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);

    /** @param persistent false for in-memory databases, whose data disappears when the application stops */
    public record Status(boolean connected, String product, String where, boolean persistent, String migration,
                         long facilities, long lanes, long disruptions, long users, String problem) {}

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public SystemStatusService(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    public Status current() {
        try (Connection c = dataSource.getConnection()) {
            if (!c.isValid(2)) return down("The database did not answer within 2 seconds.");
            String product = c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion();
            String url = c.getMetaData().getURL();
            boolean memory = url != null && url.contains(":mem:");
            String where = url == null ? "unknown" : url.replaceAll("(?i)(password|user)=[^;&]*", "$1=***").replaceAll("//[^/@]*@", "//");
            return new Status(true, product, where, !memory, scalar("SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1", String.class, "unknown"),
                    count("supply_chain_nodes"), count("transportation_lanes"), count("disruptions"), count("user_profiles"), null);
        } catch (Exception e) {
            log.warn("Status check failed: {}", e.toString());
            return down("The database is not reachable (" + e.getClass().getSimpleName() + ").");
        }
    }

    private static Status down(String why) { return new Status(false, "unavailable", "-", false, "-", 0, 0, 0, 0, why); }

    private long count(String table) {
        Long n = scalar("SELECT COUNT(*) FROM " + table, Long.class, 0L);
        return n == null ? 0 : n;
    }

    private <T> T scalar(String sql, Class<T> type, T fallback) {
        try {
            return jdbc.queryForObject(sql, type);
        } catch (org.springframework.dao.DataAccessException e) {
            log.debug("Status query failed: {}", e.getMessage());
            return fallback;
        }
    }
}
