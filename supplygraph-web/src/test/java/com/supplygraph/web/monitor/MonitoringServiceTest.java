package com.supplygraph.web.monitor;

import com.supplygraph.application.error.ErrorClassifier;
import com.supplygraph.domain.error.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Monitoring: measured checks, never invented")
class MonitoringServiceTest {
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        RecentErrors.clear();
        jdbc = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbc.queryForObject(Mockito.anyString(), Mockito.eq(Integer.class))).thenReturn(1);
        Mockito.when(jdbc.queryForObject(Mockito.anyString(), Mockito.eq(Long.class))).thenReturn(3L);
    }

    @Test
    @DisplayName("Request statistics come from recorded requests: count, average, 95th percentile, error classes, areas")
    void requestStatistics() {
        var m = new MonitoringService(jdbc);
        for (int i = 1; i <= 20; i++) m.recordRequest("Page loads", i * 10, 200);
        m.recordRequest("REST API", 50, 404);
        m.recordRequest("REST API", 70, 500);
        var r = m.snapshot().requests();
        assertThat(r.last5Min()).isEqualTo(22);
        assertThat(r.serverErrors()).isEqualTo(1);
        assertThat(r.clientErrors()).isEqualTo(1);
        assertThat(r.byArea()).containsEntry("Page loads", 20).containsEntry("REST API", 2);
        assertThat(r.p95Ms()).isBetween(150.0, 200.0);
    }

    @Test
    @DisplayName("A database that does not answer is reported as DOWN, and the overall state follows the worst check")
    void databaseDown() {
        Mockito.when(jdbc.queryForObject(Mockito.anyString(), Mockito.eq(Integer.class))).thenThrow(new DataAccessResourceFailureException("no connection"));
        var s = new MonitoringService(jdbc).snapshot();
        assertThat(s.checks()).anySatisfy(c -> { assertThat(c.name()).isEqualTo("Database"); assertThat(c.level()).isEqualTo(MonitoringService.Level.DOWN); });
        assertThat(s.overall()).isEqualTo(MonitoringService.Level.DOWN);
    }

    @Test
    @DisplayName("Classified errors appear in the recent-errors feed and count against the Errors check")
    void errorsFeed() {
        RecentErrors.record("UI", ErrorClassifier.classify(new ConflictException("stale plan")));
        var s = new MonitoringService(jdbc).snapshot();
        assertThat(s.errors()).hasSize(1);
        assertThat(s.errors().get(0).code()).isEqualTo("CONFLICT");
        assertThat(s.checks()).anySatisfy(c -> assertThat(c.detail()).startsWith("1 error(s)"));
    }

    @Test
    @DisplayName("Business counters are read from the database, not assumed")
    void businessCounters() {
        var b = new MonitoringService(jdbc).snapshot().business();
        assertThat(b.facilities()).isEqualTo(3);
        assertThat(b.outboxDead()).isEqualTo(3);
    }
}
