package com.supplygraph.adapter.persistence;

import com.supplygraph.adapter.persistence.adapter.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase13PersistenceIntegrationTest.TestConfig.class)
@Testcontainers
@DisplayName("Phase 13 Supplier Collaboration PostgreSQL Persistence Integration Test")
class Phase13PersistenceIntegrationTest {

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

    @Autowired private PostgresPartnerOrganizationPersistenceAdapter partnerAdapter;
    @Autowired private PostgresRfqPersistenceAdapter rfqAdapter;
    @Autowired private PostgresOrderCollaborationPersistenceAdapter orderAdapter;
    @Autowired private PostgresSupplierQualityPersistenceAdapter qualityAdapter;
    @Autowired private PostgresPartnerDocumentPersistenceAdapter docAdapter;

    private final WorkspaceId workspaceId = new WorkspaceId("ws-p13-collab");

    @Test
    @DisplayName("Partner organization persistence round-trip")
    void testPartnerOrganizationPersistence() {
        PartnerOrganization org = new PartnerOrganization(
                PartnerOrganizationId.generate(),
                workspaceId,
                "Panasonic Energy Europe",
                PartnerType.SUPPLIER,
                PartnerTier.TIER_1_DIRECT,
                "11-222-3333",
                "DE",
                true,
                Instant.now()
        );

        partnerAdapter.save(org);

        Optional<PartnerOrganization> loaded = partnerAdapter.findById(org.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().name()).isEqualTo("Panasonic Energy Europe");
        assertThat(loaded.get().tier()).isEqualTo(PartnerTier.TIER_1_DIRECT);
    }

    @Test
    @DisplayName("RFQ document and embedded quotations persistence round-trip")
    void testRfqPersistence() {
        RfqId rfqId = RfqId.generate();
        RfqQuotation q1 = new RfqQuotation(
                RfqQuotationId.generate(),
                rfqId,
                PartnerOrganizationId.generate(),
                BigDecimal.valueOf(18.75),
                12,
                1000.0,
                LocalDate.now().plusDays(30),
                QuotationStatus.SUBMITTED,
                Instant.now()
        );

        RfqDocument doc = new RfqDocument(
                rfqId,
                workspaceId,
                "Graphite Anode Sourcing 2026",
                new MaterialId("MAT-ANODE-GRAPHITE"),
                25000.0,
                LocalDate.now().plusDays(15),
                RfqStatus.PUBLISHED,
                List.of(q1),
                Instant.now()
        );

        rfqAdapter.save(doc);

        Optional<RfqDocument> loaded = rfqAdapter.findById(rfqId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().quotations()).hasSize(1);
        assertThat(loaded.get().quotations().get(0).unitPriceUsd()).isEqualByComparingTo(BigDecimal.valueOf(18.75));
    }

    @Test
    @DisplayName("PO collaboration record and ASN persistence round-trip")
    void testPoAndAsnPersistence() {
        PoCollaborationId poId = PoCollaborationId.generate();
        PoCollaborationRecord po = new PoCollaborationRecord(
                poId,
                workspaceId,
                "PO-2026-COLLAB-01",
                PartnerOrganizationId.generate(),
                new MaterialId("MAT-COPPER-FOIL"),
                5000.0,
                BigDecimal.valueOf(42000.0),
                LocalDate.now().plusDays(14),
                PoStatus.ISSUED,
                Instant.now()
        );

        orderAdapter.savePo(po);

        Optional<PoCollaborationRecord> loadedPo = orderAdapter.findPoById(poId);
        assertThat(loadedPo).isPresent();
        assertThat(loadedPo.get().poNumber()).isEqualTo("PO-2026-COLLAB-01");

        AdvanceShippingNotice asn = new AdvanceShippingNotice(
                AdvanceShippingNoticeId.generate(),
                workspaceId,
                poId,
                "ASN-TRK-990011",
                "FedEx Freight",
                5000.0,
                LocalDate.now().plusDays(4),
                Instant.now()
        );

        orderAdapter.saveAsn(asn);

        Optional<AdvanceShippingNotice> loadedAsn = orderAdapter.findAsnById(asn.id());
        assertThat(loadedAsn).isPresent();
        assertThat(loadedAsn.get().carrierName()).isEqualTo("FedEx Freight");
    }

    @Test
    @DisplayName("Quality 8D incident persistence round-trip")
    void testQuality8dPersistence() {
        QualityIncident8D incident = new QualityIncident8D(
                QualityIncident8DId.generate(),
                workspaceId,
                PartnerOrganizationId.generate(),
                new MaterialId("MAT-BATTERY-BUSBAR"),
                "Conductivity drop in welded copper busbar",
                IncidentSeverity.CRITICAL,
                EightDStep.D2_PROBLEM_DESCRIPTION,
                false,
                Instant.now()
        );

        qualityAdapter.save(incident);

        Optional<QualityIncident8D> loaded = qualityAdapter.findById(incident.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().defectTitle()).contains("Conductivity drop");
        assertThat(loaded.get().currentStep()).isEqualTo(EightDStep.D2_PROBLEM_DESCRIPTION);
    }

    @Test
    @DisplayName("Partner document persistence round-trip")
    void testPartnerDocumentPersistence() {
        PartnerOrganizationId partnerId = PartnerOrganizationId.generate();
        PartnerDocumentRecord doc = new PartnerDocumentRecord(
                PartnerDocumentId.generate(),
                workspaceId,
                partnerId,
                "Material-Safety-Data-Sheet-NMC811.pdf",
                DocumentCategory.SAFETY_DATA_SHEET,
                "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
                true,
                Instant.now()
        );

        docAdapter.save(doc);

        Optional<PartnerDocumentRecord> loaded = docAdapter.findById(doc.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().fileName()).isEqualTo("Material-Safety-Data-Sheet-NMC811.pdf");
        assertThat(loaded.get().verified()).isTrue();
    }
}

