package com.supplygraph.domain.model.platform;

import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformDomainTest {

    private final WorkspaceId workspaceId = new WorkspaceId("ws-test");

    @Test
    @DisplayName("IndustryPolicyRecord lifecycle and status transitions")
    void testIndustryPolicyRecord() {
        IndustryPolicyId id = IndustryPolicyId.generate();
        IndustryPolicyRecord policy = new IndustryPolicyRecord(
                id, workspaceId, "EV_BATTERY", 1, "{\"maxLeadTimeDays\":30}", PolicyStatus.DRAFT, Instant.now()
        );

        assertThat(policy.id()).isEqualTo(id);
        assertThat(policy.status()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(policy.versionNumber()).isEqualTo(1);

        IndustryPolicyRecord activePolicy = policy.withStatus(PolicyStatus.ACTIVE);
        assertThat(activePolicy.status()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(activePolicy.versionNumber()).isEqualTo(1);

        assertThatThrownBy(() -> new IndustryPolicyRecord(
                id, workspaceId, "EV_BATTERY", 0, "{}", PolicyStatus.DRAFT, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("WorkspaceQuotaRecord limits and enablement")
    void testWorkspaceQuotaRecord() {
        WorkspaceQuotaId id = WorkspaceQuotaId.generate();
        WorkspaceQuotaRecord quota = new WorkspaceQuotaRecord(
                id, workspaceId, 500, 50, 10, true, Instant.now()
        );

        assertThat(quota.maxNodes()).isEqualTo(500);
        assertThat(quota.enabled()).isTrue();

        WorkspaceQuotaRecord disabled = quota.withEnabled(false);
        assertThat(disabled.enabled()).isFalse();

        WorkspaceQuotaRecord adjusted = quota.withLimits(1000, 100, 20);
        assertThat(adjusted.maxNodes()).isEqualTo(1000);
        assertThat(adjusted.maxActiveDisruptions()).isEqualTo(100);
        assertThat(adjusted.maxConcurrentSimulations()).isEqualTo(20);

        assertThatThrownBy(() -> new WorkspaceQuotaRecord(
                id, workspaceId, -1, 10, 10, true, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ApiKeyRecord immutable scopes and status")
    void testApiKeyRecord() {
        ApiKeyId id = ApiKeyId.generate();
        List<ApiScope> scopes = List.of(ApiScope.READ_TWIN, ApiScope.EXECUTE_SIMULATION);
        ApiKeyRecord apiKey = new ApiKeyRecord(
                id, workspaceId, "Integration Client", "hash123", scopes, true, Instant.now()
        );

        assertThat(apiKey.scopes()).containsExactly(ApiScope.READ_TWIN, ApiScope.EXECUTE_SIMULATION);
        assertThat(apiKey.active()).isTrue();

        ApiKeyRecord deactivated = apiKey.withActive(false);
        assertThat(deactivated.active()).isFalse();

        assertThatThrownBy(() -> apiKey.scopes().add(ApiScope.ADMIN_ALL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("BackupSnapshotRecord verification and attributes")
    void testBackupSnapshotRecord() {
        BackupSnapshotId id = BackupSnapshotId.generate();
        BackupSnapshotRecord snapshot = new BackupSnapshotRecord(
                id, workspaceId, "weekly-backup-20261118", 1048576L, "sha256-abcdef", false, Instant.now()
        );

        assertThat(snapshot.byteSize()).isEqualTo(1048576L);
        assertThat(snapshot.verified()).isFalse();

        BackupSnapshotRecord verified = snapshot.withVerified(true);
        assertThat(verified.verified()).isTrue();

        assertThatThrownBy(() -> new BackupSnapshotRecord(
                id, workspaceId, "snap", -10L, "checksum", false, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

