package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.GenealogyDagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataGenealogyDagRepository extends JpaRepository<GenealogyDagJpaEntity, String> {
}

