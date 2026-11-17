package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.UserProfileJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataUserProfileRepository;
import com.supplygraph.application.port.out.UserProfileRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PostgresUserProfileRepositoryAdapter implements UserProfileRepositoryPort {

    private final SpringDataUserProfileRepository repository;

    public PostgresUserProfileRepositoryAdapter(SpringDataUserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserProfile> findById(UserId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public List<UserProfile> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        String workspacesCsv = userProfile.assignedWorkspaces().stream()
                .map(WorkspaceId::value)
                .collect(Collectors.joining(","));

        UserProfileJpaEntity entity = new UserProfileJpaEntity(
                userProfile.id().value(),
                userProfile.username(),
                userProfile.email(),
                userProfile.displayName(),
                userProfile.role().name(),
                workspacesCsv,
                userProfile.active(),
                userProfile.createdAt()
        );
        UserProfileJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(UserId id) {
        repository.deleteById(id.value());
    }

    private UserProfile toDomain(UserProfileJpaEntity entity) {
        Set<WorkspaceId> workspaces = new HashSet<>();
        if (entity.getAssignedWorkspaces() != null && !entity.getAssignedWorkspaces().isBlank()) {
            for (String ws : entity.getAssignedWorkspaces().split(",")) {
                if (!ws.isBlank()) {
                    workspaces.add(WorkspaceId.of(ws.trim()));
                }
            }
        }

        return new UserProfile(
                UserId.of(entity.getId()),
                entity.getUsername(),
                entity.getEmail(),
                entity.getDisplayName(),
                UserRole.valueOf(entity.getRole()),
                workspaces,
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}

