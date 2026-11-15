package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.DigitalTwinStateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDigitalTwinStateRepository extends JpaRepository<DigitalTwinStateJpaEntity, String> {
}

