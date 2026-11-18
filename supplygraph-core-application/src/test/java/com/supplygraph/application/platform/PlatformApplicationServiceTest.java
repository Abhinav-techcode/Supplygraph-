package com.supplygraph.application.platform;

import com.supplygraph.application.platform.port.out.ApiKeyRepositoryPort;
import com.supplygraph.application.platform.port.out.BackupSnapshotRepositoryPort;
import com.supplygraph.application.platform.port.out.IndustryPolicyRepositoryPort;
import com.supplygraph.application.platform.port.out.WorkspaceQuotaRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformApplicationServiceTest {

    private final WorkspaceId workspaceId = new WorkspaceId("ws-platform-test");

    private InMemoryPolicyRepo policyRepo;
    private InMemoryQuotaRepo quotaRepo;
    private InMemoryApiKeyRepo apiKeyRepo;
    private InMemorySnapshotRepo snapshotRepo;

    private IndustryPolicyLifecycleService policyService;
    private WorkspaceAdministrationService workspaceService;
    private ApiDeveloperCentreService apiService;
    private BackupDisasterRecoveryService backupService;
    private PlatformTelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        policyRepo = new InMemoryPolicyRepo();
        quotaRepo = new InMemoryQuotaRepo();
        apiKeyRepo = new InMemoryApiKeyRepo();
        snapshotRepo = new InMemorySnapshotRepo();

        policyService = new IndustryPolicyLifecycleService(policyRepo);
        workspaceService = new WorkspaceAdministrationService(quotaRepo);
        apiService = new ApiDeveloperCentreService(apiKeyRepo);
        backupService = new BackupDisasterRecoveryService(snapshotRepo);
        telemetryService = new PlatformTelemetryService();
    }

    @Test
    @DisplayName("IndustryPolicyLifecycleService creates, validates, activates and versions policies")
    void testPolicyLifecycle() {
        IndustryPolicyRecord draft1 = policyService.createDraftPolicy(workspaceId, "SEMICONDUCTOR", "{\"safetyStock\":100}");
        assertThat(draft1.versionNumber()).isEqualTo(1);
        assertThat(draft1.status()).isEqualTo(PolicyStatus.DRAFT);

        IndustryPolicyRecord validated = policyService.validatePolicy(draft1.id());
        assertThat(validated.status()).isEqualTo(PolicyStatus.VALIDATED);

        IndustryPolicyRecord active1 = policyService.activatePolicy(draft1.id());
        assertThat(active1.status()).isEqualTo(PolicyStatus.ACTIVE);

        // Create version 2
        IndustryPolicyRecord draft2 = policyService.createDraftPolicy(workspaceId, "SEMICONDUCTOR", "{\"safetyStock\":200}");
        assertThat(draft2.versionNumber()).isEqualTo(2);

        IndustryPolicyRecord active2 = policyService.activatePolicy(draft2.id());
        assertThat(active2.status()).isEqualTo(PolicyStatus.ACTIVE);

        // Previous active policy should now be deprecated
        assertThat(policyRepo.findById(draft1.id()).get().status()).isEqualTo(PolicyStatus.DEPRECATED);

        // Rollback to version 1
        IndustryPolicyRecord rolledBack = policyService.rollbackPolicy(draft1.id());
        assertThat(rolledBack.versionNumber()).isEqualTo(3);
        assertThat(rolledBack.status()).isEqualTo(PolicyStatus.ACTIVE);

        List<IndustryPolicyRecord> history = policyService.getPolicyHistory(workspaceId, "SEMICONDUCTOR");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).versionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("IndustryPolicyLifecycleService diffs two policy configurations")
    void testPolicyComparison() {
        IndustryPolicyRecord p1 = policyService.createDraftPolicy(workspaceId, "AEROSPACE", "{\"tolerance\":0.01}");
        IndustryPolicyRecord p2 = policyService.createDraftPolicy(workspaceId, "AEROSPACE", "{\"tolerance\":0.05, \"extended\": true}");

        var diff = policyService.comparePolicies(p1.id(), p2.id());
        assertThat(diff.identicalJson()).isFalse();
        assertThat(diff.lengthDifference()).isGreaterThan(0);
    }

    @Test
    @DisplayName("WorkspaceAdministrationService manages quotas and allowance enforcement")
    void testWorkspaceQuotas() {
        WorkspaceQuotaRecord quota = workspaceService.getOrCreateQuota(workspaceId);
        assertThat(quota.enabled()).isTrue();
        assertThat(quota.maxNodes()).isEqualTo(1000);

        workspaceService.configureQuota(workspaceId, 200, 20, 5);
        assertThat(workspaceService.checkQuotaAllowance(workspaceId, "NODES", 150)).isTrue();
        assertThat(workspaceService.checkQuotaAllowance(workspaceId, "NODES", 200)).isFalse();

        workspaceService.toggleWorkspace(workspaceId, false);
        assertThat(workspaceService.checkQuotaAllowance(workspaceId, "NODES", 50)).isFalse();
    }

    @Test
    @DisplayName("ApiDeveloperCentreService generates, hashes, and validates scoped API keys")
    void testApiKeys() {
        var result = apiService.generateApiKey(workspaceId, "ERP Webhook", List.of(ApiScope.READ_TWIN, ApiScope.MUTATE_PLAN));
        String rawKey = result.rawApiKey();
        assertThat(rawKey).startsWith("sg_live_");

        assertThat(apiService.validateApiKey(rawKey, ApiScope.READ_TWIN)).isTrue();
        assertThat(apiService.validateApiKey(rawKey, ApiScope.MUTATE_PLAN)).isTrue();
        assertThat(apiService.validateApiKey(rawKey, ApiScope.ADMIN_ALL)).isFalse();

        apiService.revokeApiKey(result.record().id());
        assertThat(apiService.validateApiKey(rawKey, ApiScope.READ_TWIN)).isFalse();
    }

    @Test
    @DisplayName("BackupDisasterRecoveryService creates, verifies, and restores snapshots")
    void testBackupAndRestore() {
        BackupSnapshotRecord snapshot = backupService.createBackupSnapshot(workspaceId, "pre-migration-dump");
        assertThat(snapshot.verified()).isFalse();

        var failedRestore = backupService.restoreSnapshot(snapshot.id());
        assertThat(failedRestore.success()).isFalse();

        backupService.verifySnapshot(snapshot.id());
        var verifiedRestore = backupService.restoreSnapshot(snapshot.id());
        assertThat(verifiedRestore.success()).isTrue();
    }

    @Test
    @DisplayName("PlatformTelemetryService returns active system and pool telemetry")
    void testTelemetry() {
        var sys = telemetryService.getSystemMetrics();
        assertThat(sys.availableProcessors()).isGreaterThan(0);
        assertThat(sys.totalMemoryBytes()).isGreaterThan(0);

        var pool = telemetryService.getDatabasePoolMetrics();
        assertThat(pool.maxPoolSize()).isEqualTo(20);
    }

    // --- In-Memory Repository Fakes ---
    static class InMemoryPolicyRepo implements IndustryPolicyRepositoryPort {
        private final Map<IndustryPolicyId, IndustryPolicyRecord> store = new HashMap<>();

        @Override
        public void save(IndustryPolicyRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<IndustryPolicyRecord> findById(IndustryPolicyId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<IndustryPolicyRecord> findByWorkspaceId(WorkspaceId workspaceId) {
            return store.values().stream().filter(p -> p.workspaceId().equals(workspaceId)).toList();
        }

        @Override
        public List<IndustryPolicyRecord> findByWorkspaceIdAndIndustryCode(WorkspaceId workspaceId, String industryCode) {
            return store.values().stream()
                    .filter(p -> p.workspaceId().equals(workspaceId) && p.industryCode().equalsIgnoreCase(industryCode))
                    .toList();
        }
    }

    static class InMemoryQuotaRepo implements WorkspaceQuotaRepositoryPort {
        private final Map<WorkspaceId, WorkspaceQuotaRecord> store = new HashMap<>();

        @Override
        public void save(WorkspaceQuotaRecord record) {
            store.put(record.workspaceId(), record);
        }

        @Override
        public Optional<WorkspaceQuotaRecord> findById(WorkspaceQuotaId id) {
            return store.values().stream().filter(q -> q.id().equals(id)).findFirst();
        }

        @Override
        public Optional<WorkspaceQuotaRecord> findByWorkspaceId(WorkspaceId workspaceId) {
            return Optional.ofNullable(store.get(workspaceId));
        }
    }

    static class InMemoryApiKeyRepo implements ApiKeyRepositoryPort {
        private final Map<ApiKeyId, ApiKeyRecord> store = new HashMap<>();

        @Override
        public void save(ApiKeyRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<ApiKeyRecord> findById(ApiKeyId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ApiKeyRecord> findByWorkspaceId(WorkspaceId workspaceId) {
            return store.values().stream().filter(k -> k.workspaceId().equals(workspaceId)).toList();
        }

        @Override
        public Optional<ApiKeyRecord> findByHashedToken(String hashedToken) {
            return store.values().stream().filter(k -> k.hashedToken().equals(hashedToken)).findFirst();
        }
    }

    static class InMemorySnapshotRepo implements BackupSnapshotRepositoryPort {
        private final Map<BackupSnapshotId, BackupSnapshotRecord> store = new HashMap<>();

        @Override
        public void save(BackupSnapshotRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<BackupSnapshotRecord> findById(BackupSnapshotId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<BackupSnapshotRecord> findByWorkspaceId(WorkspaceId workspaceId) {
            return store.values().stream().filter(s -> s.workspaceId().equals(workspaceId)).toList();
        }
    }
}

