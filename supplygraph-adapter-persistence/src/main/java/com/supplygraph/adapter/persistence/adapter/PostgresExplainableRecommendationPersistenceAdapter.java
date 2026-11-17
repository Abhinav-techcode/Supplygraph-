package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ExplainableRecommendationJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataExplainableRecommendationRepository;
import com.supplygraph.application.port.out.ExplainableRecommendationRepositoryPort;
import com.supplygraph.domain.model.explainability.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresExplainableRecommendationPersistenceAdapter implements ExplainableRecommendationRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(PostgresExplainableRecommendationPersistenceAdapter.class);

    private final SpringDataExplainableRecommendationRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresExplainableRecommendationPersistenceAdapter(SpringDataExplainableRecommendationRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(ExplainableRecommendation rec) {
        String actionsJson = serialize(rec.primaryActions());
        String evidenceJson = serialize(rec.evidenceList());
        String constraintsJson = serialize(rec.constraintsEvaluated());
        String tradeoffsJson = serialize(rec.tradeOffs());
        String uncertaintyJson = serialize(rec.uncertaintyFactors());

        ExplainableRecommendationJpaEntity entity = new ExplainableRecommendationJpaEntity(
                rec.id().value(),
                rec.disruptionId(),
                rec.stateVersion(),
                rec.title(),
                rec.summary(),
                actionsJson,
                rec.expectedCostUsd(),
                rec.expectedRecoveryTimeHours(),
                rec.serviceLevelRestorationPct(),
                evidenceJson,
                constraintsJson,
                tradeoffsJson,
                uncertaintyJson,
                rec.requiresHumanApproval(),
                rec.approved(),
                rec.approvedBy(),
                rec.approvedAt(),
                rec.rejectionReason()
        );

        repository.save(entity);
    }

    @Override
    public Optional<ExplainableRecommendation> findById(RecommendationId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ExplainableRecommendation> findByDisruptionId(String disruptionId) {
        return repository.findByDisruptionId(disruptionId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ExplainableRecommendation> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private ExplainableRecommendation toDomain(ExplainableRecommendationJpaEntity e) {
        List<String> actions = deserialize(e.getPrimaryActions(), new TypeReference<List<String>>() {}, List.of());
        List<DecisionEvidence> evidence = deserialize(e.getEvidenceJson(), new TypeReference<List<DecisionEvidence>>() {}, List.of());
        List<DecisionConstraint> constraints = deserialize(e.getConstraintsJson(), new TypeReference<List<DecisionConstraint>>() {}, List.of());
        List<DecisionTradeOff> tradeoffs = deserialize(e.getTradeoffsJson(), new TypeReference<List<DecisionTradeOff>>() {}, List.of());
        Map<String, String> uncertainty = deserialize(e.getUncertaintyJson(), new TypeReference<Map<String, String>>() {}, Map.of());

        return new ExplainableRecommendation(
                RecommendationId.of(e.getId()),
                e.getDisruptionId(),
                e.getStateVersion(),
                e.getTitle(),
                e.getSummary(),
                actions,
                e.getExpectedCostUsd(),
                e.getExpectedRecoveryTimeHours(),
                e.getServiceRestorationPct(),
                evidence,
                constraints,
                tradeoffs,
                uncertainty,
                e.isRequiresHumanApproval(),
                e.isApproved(),
                e.getApprovedBy(),
                e.getApprovedAt(),
                e.getRejectionReason()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef, T defaultVal) {
        if (json == null || json.isBlank()) return defaultVal;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
