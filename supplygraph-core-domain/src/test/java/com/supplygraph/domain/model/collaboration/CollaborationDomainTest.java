package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 13 Collaboration Domain Models Test")
class CollaborationDomainTest {

    @Test
    @DisplayName("PartnerOrganization creation and attributes")
    void testPartnerOrganization() {
        PartnerOrganization partner = new PartnerOrganization(
                PartnerOrganizationId.generate(),
                new WorkspaceId("ws-collab"),
                "CATL Battery Cell Co.",
                PartnerType.SUPPLIER,
                PartnerTier.TIER_1_DIRECT,
                "123-456-789",
                "CN",
                true,
                Instant.now()
        );

        assertThat(partner.name()).isEqualTo("CATL Battery Cell Co.");
        assertThat(partner.tier()).isEqualTo(PartnerTier.TIER_1_DIRECT);
        assertThat(partner.active()).isTrue();
    }

    @Test
    @DisplayName("RfqDocument and RfqQuotation lifecycle")
    void testRfqLifecycle() {
        RfqId rfqId = RfqId.generate();
        RfqDocument doc = new RfqDocument(
                rfqId,
                new WorkspaceId("ws-collab"),
                "NMC 811 Cathode Material Sourcing",
                new MaterialId("MAT-CATHODE-NMC"),
                10000.0,
                LocalDate.now().plusDays(30),
                RfqStatus.PUBLISHED,
                null,
                Instant.now()
        );

        RfqQuotation quote = new RfqQuotation(
                RfqQuotationId.generate(),
                rfqId,
                PartnerOrganizationId.generate(),
                BigDecimal.valueOf(28.50),
                14,
                500.0,
                LocalDate.now().plusDays(60),
                QuotationStatus.SUBMITTED,
                Instant.now()
        );

        RfqDocument updatedDoc = doc.withQuotation(quote);
        assertThat(updatedDoc.quotations()).hasSize(1);
        assertThat(updatedDoc.findQuotation(quote.id())).isPresent();

        RfqDocument awardedDoc = updatedDoc.withStatus(RfqStatus.AWARDED);
        assertThat(awardedDoc.status()).isEqualTo(RfqStatus.AWARDED);
    }

    @Test
    @DisplayName("PoCollaborationRecord status progression and ASN generation")
    void testPoCollaborationAndAsn() {
        PoCollaborationId poId = PoCollaborationId.generate();
        PoCollaborationRecord po = new PoCollaborationRecord(
                poId,
                new WorkspaceId("ws-collab"),
                "PO-2026-9901",
                PartnerOrganizationId.generate(),
                new MaterialId("MAT-CATHODE-NMC"),
                2000.0,
                BigDecimal.valueOf(57000.00),
                LocalDate.now().plusDays(21),
                PoStatus.ISSUED,
                Instant.now()
        );

        PoCollaborationRecord acknowledged = po.withStatus(PoStatus.ACKNOWLEDGED, LocalDate.now().plusDays(20));
        assertThat(acknowledged.status()).isEqualTo(PoStatus.ACKNOWLEDGED);

        AdvanceShippingNotice asn = new AdvanceShippingNotice(
                AdvanceShippingNoticeId.generate(),
                new WorkspaceId("ws-collab"),
                poId,
                "TRK-981726354",
                "DHL Global Forwarding",
                2000.0,
                LocalDate.now().plusDays(5),
                Instant.now()
        );

        assertThat(asn.poId()).isEqualTo(poId);
        assertThat(asn.shippedQuantity()).isEqualTo(2000.0);
    }

    @Test
    @DisplayName("QualityIncident8D step progression to closure")
    void test8DIncidentProgression() {
        QualityIncident8D incident = new QualityIncident8D(
                QualityIncident8DId.generate(),
                new WorkspaceId("ws-collab"),
                PartnerOrganizationId.generate(),
                new MaterialId("MAT-CELL-HOUSING"),
                "Dimensional tolerance breach in cell casing",
                IncidentSeverity.HIGH,
                EightDStep.D1_TEAM_FORMATION,
                false,
                Instant.now()
        );

        QualityIncident8D advanced = incident.advanceStep(EightDStep.D3_CONTAINMENT_ACTIONS);
        assertThat(advanced.currentStep()).isEqualTo(EightDStep.D3_CONTAINMENT_ACTIONS);
        assertThat(advanced.closed()).isFalse();

        QualityIncident8D closed = advanced.advanceStep(EightDStep.D8_CONGRATULATE_TEAM);
        assertThat(closed.currentStep()).isEqualTo(EightDStep.D8_CONGRATULATE_TEAM);
        assertThat(closed.closed()).isTrue();
    }
}

