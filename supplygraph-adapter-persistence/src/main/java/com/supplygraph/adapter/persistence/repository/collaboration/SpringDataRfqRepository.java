package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.RfqDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataRfqRepository extends JpaRepository<RfqDocumentJpaEntity, String> {
    List<RfqDocumentJpaEntity> findByWorkspaceId(String workspaceId);
}

