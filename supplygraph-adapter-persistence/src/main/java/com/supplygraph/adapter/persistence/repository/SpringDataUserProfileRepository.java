package com.supplygraph.adapter.persistence.repository;

import com.supplygraph.adapter.persistence.entity.UserProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileJpaEntity, String> {
    Optional<UserProfileJpaEntity> findByUsername(String username);
}

