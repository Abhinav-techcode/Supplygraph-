package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/** Native-only repository for the composite-key inbox table (bound to an existing entity type for Spring Data). */
@Repository
public interface SpringDataInboxRepository extends JpaRepository<OutboxEventJpaEntity, String> {
    @Modifying
    @Query(value = "INSERT INTO inbox_messages (workspace_id, consumer, message_id, processed_at) VALUES (:ws, :consumer, :msg, :now) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    int registerIfAbsent(@Param("ws") String ws, @Param("consumer") String consumer, @Param("msg") String msg, @Param("now") Instant now);
}
