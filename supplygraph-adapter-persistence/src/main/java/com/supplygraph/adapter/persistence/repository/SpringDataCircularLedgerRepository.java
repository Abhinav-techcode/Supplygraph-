package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.CircularLedgerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCircularLedgerRepository extends JpaRepository<CircularLedgerJpaEntity, String> {
    List<CircularLedgerJpaEntity> findByWorkspaceId(String workspaceId);
}

