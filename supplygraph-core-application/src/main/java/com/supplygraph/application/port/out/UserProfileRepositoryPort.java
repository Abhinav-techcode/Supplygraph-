package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for persisting and querying enterprise user profiles.
 */
public interface UserProfileRepositoryPort {

    Optional<UserProfile> findById(UserId id);

    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findAll();

    UserProfile save(UserProfile userProfile);

    void deleteById(UserId id);
}

