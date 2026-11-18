package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ReconciliationItemJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataReconciliationItemRepository extends JpaRepository<ReconciliationItemJpaEntity, String> {
    List<ReconciliationItemJpaEntity> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(String ws, String status);
    List<ReconciliationItemJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(String ws, Pageable page);
    boolean existsByWorkspaceIdAndConnectorIdAndEntityTypeAndEntityKeyAndFieldNameAndExternalValueAndStatus(String ws, String c, String t, String k, String f, String v, String s);
}
