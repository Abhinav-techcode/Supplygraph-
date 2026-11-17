package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.ReplanningRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataReplanningRunRepository extends JpaRepository<ReplanningRunJpaEntity, String> {
    List<ReplanningRunJpaEntity> findByWorkspaceIdOrderByStartedAtDesc(String workspaceId);
}

