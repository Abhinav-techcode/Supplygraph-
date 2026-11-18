package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.SyncCheckpointJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSyncCheckpointRepository extends JpaRepository<SyncCheckpointJpaEntity, String> {

}
