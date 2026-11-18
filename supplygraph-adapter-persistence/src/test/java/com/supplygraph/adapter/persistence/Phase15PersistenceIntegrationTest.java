package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase15PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 15 Enterprise Platform PostgreSQL Persistence Integration Test")
class Phase15PersistenceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.supplygraph.adapter.persistence")
    @EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
    static class TestConfig {}

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplygraph_test")
            .withUsername("sg_test")
            .withPassword("sg_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private PostgresIndustryPolicyPersistenceAdapter policyAdapter;
    @Autowired private PostgresWorkspaceQuotaPersistenceAdapter quotaAdapter;
    @Autowired private PostgresApiKeyPersistenceAdapter apiKeyAdapter;
    @Autowired private PostgresBackupSnapshotPersistenceAdapter snapshotAdapter;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-p15-test");

    @Test
    @DisplayName("Persist and query Industry Policy records")
    void testIndustryPolicyPersistence() {
        IndustryPolicyId id = IndustryPolicyId.generate();
        IndustryPolicyRecord policy = new IndustryPolicyRecord(
                id, workspaceId, "PHARMACEUTICALS", 1, "{\"coldChainTempMin\":2,\"coldChainTempMax\":8}", PolicyStatus.ACTIVE, Instant.now()
        );

        policyAdapter.save(policy);

        Optional<IndustryPolicyRecord> retrieved = policyAdapter.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().industryCode()).isEqualTo("PHARMACEUTICALS");
        assertThat(retrieved.get().status()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(retrieved.get().versionNumber()).isEqualTo(1);

        List<IndustryPolicyRecord> byWs = policyAdapter.findByWorkspaceId(workspaceId);
        assertThat(byWs).extracting(IndustryPolicyRecord::id).contains(id);

        List<IndustryPolicyRecord> byInd = policyAdapter.findByWorkspaceIdAndIndustryCode(workspaceId, "PHARMACEUTICALS");
        assertThat(byInd).hasSize(1);
    }

    @Test
    @DisplayName("Persist and query Workspace Quota records")
    void testWorkspaceQuotaPersistence() {
        WorkspaceQuotaId id = WorkspaceQuotaId.generate();
        WorkspaceQuotaRecord quota = new WorkspaceQuotaRecord(
                id, workspaceId, 5000, 200, 50, true, Instant.now()
        );

        quotaAdapter.save(quota);

        Optional<WorkspaceQuotaRecord> retrieved = quotaAdapter.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().maxNodes()).isEqualTo(5000);
        assertThat(retrieved.get().maxActiveDisruptions()).isEqualTo(200);
        assertThat(retrieved.get().enabled()).isTrue();

        Optional<WorkspaceQuotaRecord> byWs = quotaAdapter.findByWorkspaceId(workspaceId);
        assertThat(byWs).isPresent();
        assertThat(byWs.get().id()).isEqualTo(id);
    }

    @Test
    @DisplayName("Persist and query ApiKey records by ID and hashed token")
    void testApiKeyPersistence() {
        ApiKeyId id = ApiKeyId.generate();
        String hashedToken = "hash-token-p15-998877";
        ApiKeyRecord apiKey = new ApiKeyRecord(
                id, workspaceId, "CI/CD Deployment Key", hashedToken, List.of(ApiScope.READ_TWIN, ApiScope.EXECUTE_SIMULATION), true, Instant.now()
        );

        apiKeyAdapter.save(apiKey);

        Optional<ApiKeyRecord> retrieved = apiKeyAdapter.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().keyName()).isEqualTo("CI/CD Deployment Key");
        assertThat(retrieved.get().scopes()).containsExactly(ApiScope.READ_TWIN, ApiScope.EXECUTE_SIMULATION);

        Optional<ApiKeyRecord> byHash = apiKeyAdapter.findByHashedToken(hashedToken);
        assertThat(byHash).isPresent();
        assertThat(byHash.get().id()).isEqualTo(id);

        List<ApiKeyRecord> byWs = apiKeyAdapter.findByWorkspaceId(workspaceId);
        assertThat(byWs).extracting(ApiKeyRecord::id).contains(id);
    }

    @Test
    @DisplayName("Persist and query Backup Snapshot records")
    void testBackupSnapshotPersistence() {
        BackupSnapshotId id = BackupSnapshotId.generate();
        BackupSnapshotRecord snapshot = new BackupSnapshotRecord(
                id, workspaceId, "snap-full-backup-v15", 52428800L, "checksum-sha256-abcdef123456", true, Instant.now()
        );

        snapshotAdapter.save(snapshot);

        Optional<BackupSnapshotRecord> retrieved = snapshotAdapter.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().snapshotName()).isEqualTo("snap-full-backup-v15");
        assertThat(retrieved.get().byteSize()).isEqualTo(52428800L);
        assertThat(retrieved.get().verified()).isTrue();

        List<BackupSnapshotRecord> byWs = snapshotAdapter.findByWorkspaceId(workspaceId);
        assertThat(byWs).extracting(BackupSnapshotRecord::id).contains(id);
    }
}

