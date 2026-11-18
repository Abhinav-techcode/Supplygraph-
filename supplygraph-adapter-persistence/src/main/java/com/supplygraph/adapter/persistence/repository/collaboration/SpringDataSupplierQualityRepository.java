package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.QualityIncident8DJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSupplierQualityRepository extends JpaRepository<QualityIncident8DJpaEntity, String> {
    List<QualityIncident8DJpaEntity> findByWorkspaceId(String workspaceId);
}

