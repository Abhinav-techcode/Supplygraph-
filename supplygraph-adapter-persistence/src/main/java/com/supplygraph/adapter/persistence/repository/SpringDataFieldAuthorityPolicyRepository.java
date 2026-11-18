package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.FieldAuthorityPolicyJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataFieldAuthorityPolicyRepository extends JpaRepository<FieldAuthorityPolicyJpaEntity, String> {
    List<FieldAuthorityPolicyJpaEntity> findByWorkspaceId(String ws);
}
