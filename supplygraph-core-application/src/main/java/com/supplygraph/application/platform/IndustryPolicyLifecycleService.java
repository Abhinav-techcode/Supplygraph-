package com.supplygraph.application.platform;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.platform.port.out.IndustryPolicyRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.IndustryPolicyId;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;
import com.supplygraph.domain.model.platform.PolicyStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class IndustryPolicyLifecycleService {

    private final IndustryPolicyRepositoryPort policyRepository;

    public IndustryPolicyLifecycleService(IndustryPolicyRepositoryPort policyRepository) {
        this.policyRepository = policyRepository;
    }

    public record PolicyComparisonResult(
            IndustryPolicyRecord policyA,
            IndustryPolicyRecord policyB,
            boolean identicalJson,
            int lengthDifference
    ) {}

    public IndustryPolicyRecord createDraftPolicy(WorkspaceId workspaceId, String industryCode, String policyJson) {
        List<IndustryPolicyRecord> existing = policyRepository.findByWorkspaceIdAndIndustryCode(workspaceId, industryCode);
        int nextVersion = existing.stream()
                .mapToInt(IndustryPolicyRecord::versionNumber)
                .max()
                .orElse(0) + 1;

        IndustryPolicyRecord record = new IndustryPolicyRecord(
                IndustryPolicyId.generate(),
                workspaceId,
                industryCode,
                nextVersion,
                policyJson,
                PolicyStatus.DRAFT,
                Instant.now()
        );
        policyRepository.save(record);
        return record;
    }

    public IndustryPolicyRecord validatePolicy(IndustryPolicyId id) {
        IndustryPolicyRecord record = policyRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Industry policy not found: " + id.value()));

        if (record.policyJson().isBlank()) {
            throw new IllegalStateException("Policy payload cannot be blank for validation");
        }

        IndustryPolicyRecord validated = record.withStatus(PolicyStatus.VALIDATED);
        policyRepository.save(validated);
        return validated;
    }

    public IndustryPolicyRecord activatePolicy(IndustryPolicyId id) {
        IndustryPolicyRecord record = policyRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Industry policy not found: " + id.value()));

        // Deprecate any currently active policies for the same workspace & industry
        List<IndustryPolicyRecord> existing = policyRepository.findByWorkspaceIdAndIndustryCode(record.workspaceId(), record.industryCode());
        for (IndustryPolicyRecord ep : existing) {
            if (ep.status() == PolicyStatus.ACTIVE && !ep.id().equals(id)) {
                policyRepository.save(ep.withStatus(PolicyStatus.DEPRECATED));
            }
        }

        IndustryPolicyRecord active = record.withStatus(PolicyStatus.ACTIVE);
        policyRepository.save(active);
        return active;
    }

    public IndustryPolicyRecord deprecatePolicy(IndustryPolicyId id) {
        IndustryPolicyRecord record = policyRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Industry policy not found: " + id.value()));

        IndustryPolicyRecord deprecated = record.withStatus(PolicyStatus.DEPRECATED);
        policyRepository.save(deprecated);
        return deprecated;
    }

    public IndustryPolicyRecord rollbackPolicy(IndustryPolicyId targetPolicyId) {
        IndustryPolicyRecord target = policyRepository.findById(targetPolicyId)
                .orElseThrow(() -> new ValidationException("Target policy not found: " + targetPolicyId.value()));

        List<IndustryPolicyRecord> existing = policyRepository.findByWorkspaceIdAndIndustryCode(target.workspaceId(), target.industryCode());
        int nextVersion = existing.stream()
                .mapToInt(IndustryPolicyRecord::versionNumber)
                .max()
                .orElse(0) + 1;

        // Rollback is tracked as a new revision with ROLLBACK status, then activated
        IndustryPolicyRecord rollbackRecord = new IndustryPolicyRecord(
                IndustryPolicyId.generate(),
                target.workspaceId(),
                target.industryCode(),
                nextVersion,
                target.policyJson(),
                PolicyStatus.ROLLBACK,
                Instant.now()
        );
        policyRepository.save(rollbackRecord);
        return activatePolicy(rollbackRecord.id());
    }

    public List<IndustryPolicyRecord> getPolicyHistory(WorkspaceId workspaceId, String industryCode) {
        return policyRepository.findByWorkspaceIdAndIndustryCode(workspaceId, industryCode).stream()
                .sorted(Comparator.comparingInt(IndustryPolicyRecord::versionNumber).reversed())
                .toList();
    }

    public List<IndustryPolicyRecord> findAll(WorkspaceId workspaceId) {
        return policyRepository.findByWorkspaceId(workspaceId);
    }

    public Optional<IndustryPolicyRecord> findById(IndustryPolicyId id) {
        return policyRepository.findById(id);
    }

    public PolicyComparisonResult comparePolicies(IndustryPolicyId idA, IndustryPolicyId idB) {
        IndustryPolicyRecord a = policyRepository.findById(idA)
                .orElseThrow(() -> new ValidationException("Policy A not found: " + idA.value()));
        IndustryPolicyRecord b = policyRepository.findById(idB)
                .orElseThrow(() -> new ValidationException("Policy B not found: " + idB.value()));

        boolean identical = a.policyJson().trim().equals(b.policyJson().trim());
        int lenDiff = Math.abs(a.policyJson().length() - b.policyJson().length());
        return new PolicyComparisonResult(a, b, identical, lenDiff);
    }
}

