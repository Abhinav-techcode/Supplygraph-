package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventJpaEntity, String> {

    @Modifying
    @Query(value = """
            INSERT INTO outbox_events (id, workspace_id, event_type, aggregate_id, payload, idempotency_key, correlation_id,
                status, attempts, next_attempt_at, last_error, created_at, updated_at, replay_count)
            VALUES (:id, :ws, :type, :agg, :payload, :key, :corr, :status, :attempts, :next, :err, :created, :updated, :replays)
            ON CONFLICT (workspace_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("ws") String ws, @Param("type") String type, @Param("agg") String agg,
                       @Param("payload") String payload, @Param("key") String key, @Param("corr") String corr,
                       @Param("status") String status, @Param("attempts") int attempts, @Param("next") Instant next,
                       @Param("err") String err, @Param("created") Instant created, @Param("updated") Instant updated,
                       @Param("replays") int replays);

    Optional<OutboxEventJpaEntity> findByWorkspaceIdAndIdempotencyKey(String workspaceId, String idempotencyKey);

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' AND next_attempt_at <= :now " +
            "ORDER BY created_at LIMIT :lim FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventJpaEntity> lockDue(@Param("now") Instant now, @Param("lim") int limit);

    @Modifying
    @Query(value = "UPDATE outbox_events SET status = 'PENDING', next_attempt_at = :now, updated_at = :now, " +
            "last_error = 'Recovered after worker interruption' WHERE status = 'IN_FLIGHT' AND updated_at < :staleBefore", nativeQuery = true)
    int recoverStale(@Param("staleBefore") Instant staleBefore, @Param("now") Instant now);

    List<OutboxEventJpaEntity> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(String ws, String status, Pageable page);
    List<OutboxEventJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String ws, Pageable page);

    @Query("SELECT e.status, COUNT(e) FROM OutboxEventJpaEntity e GROUP BY e.status")
    List<Object[]> countAllByStatus();

    @Query("SELECT e.status, COUNT(e) FROM OutboxEventJpaEntity e WHERE e.workspaceId = :ws GROUP BY e.status")
    List<Object[]> countByStatus(@Param("ws") String ws);
}
