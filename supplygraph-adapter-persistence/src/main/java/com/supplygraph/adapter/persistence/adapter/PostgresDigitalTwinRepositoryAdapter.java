package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.DigitalTwinStateJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataDigitalTwinStateRepository;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PostgresDigitalTwinRepositoryAdapter implements DigitalTwinRepositoryPort {

    private final SpringDataDigitalTwinStateRepository repository;
    private final TwinGraphStore graphStore;
    private final Map<WorkspaceId, DigitalTwinState> stateCache = new ConcurrentHashMap<>();

    public PostgresDigitalTwinRepositoryAdapter(SpringDataDigitalTwinStateRepository repository, TwinGraphStore graphStore) {
        this.repository = repository;
        this.graphStore = graphStore;
    }

    @Override
    public void save(DigitalTwinState state) {
        stateCache.put(state.getWorkspaceId(), state);
        DigitalTwinStateJpaEntity entity = new DigitalTwinStateJpaEntity(
                state.getWorkspaceId().value(),
                state.getCurrentVersion().value(),
                Instant.now()
        );
        repository.save(entity);
        graphStore.save(state.getWorkspaceId(), state);   // durable: graph, BOM and inventory balances
    }

    @Override
    public Optional<DigitalTwinState> findByWorkspaceId(WorkspaceId workspaceId) {
        DigitalTwinState cached = stateCache.get(workspaceId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return repository.findById(workspaceId.value()).map(entity -> {
            DigitalTwinState loaded = graphStore.hasGraph(workspaceId)
                    ? graphStore.load(workspaceId, entity.getVersion())
                    : new DigitalTwinState(workspaceId, StateVersion.of(entity.getVersion()), null, null);
            stateCache.put(workspaceId, loaded);
            return loaded;
        });
    }

    @Override
    public boolean existsByWorkspaceId(WorkspaceId workspaceId) {
        return stateCache.containsKey(workspaceId) || repository.existsById(workspaceId.value());
    }
}

