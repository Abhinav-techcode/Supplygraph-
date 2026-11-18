package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.FieldMirrorJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataFieldMirrorRepository extends JpaRepository<FieldMirrorJpaEntity, String> {
    List<FieldMirrorJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String ws, Pageable page);
}
