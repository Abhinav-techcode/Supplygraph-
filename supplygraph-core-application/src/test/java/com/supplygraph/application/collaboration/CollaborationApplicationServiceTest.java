package com.supplygraph.application.collaboration;

import com.supplygraph.application.collaboration.port.out.OrderCollaborationRepositoryPort;
import com.supplygraph.application.collaboration.port.out.PartnerDocumentRepositoryPort;
import com.supplygraph.application.collaboration.port.out.PartnerOrganizationRepositoryPort;
import com.supplygraph.application.collaboration.port.out.RfqRepositoryPort;
import com.supplygraph.application.collaboration.port.out.SupplierQualityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 13 Collaboration Application Services Test")
class CollaborationApplicationServiceTest {

    private final WorkspaceId workspaceId = new WorkspaceId("ws-collab-test");

    private InMemoryPartnerRepo partnerRepo;
    private InMemoryRfqRepo rfqRepo;
    private InMemoryOrderRepo orderRepo;
    private InMemoryQualityRepo qualityRepo;
    private InMemoryDocRepo docRepo;

    private SupplierPortalService portalService;
    private RfqCollaborationService rfqService;
    private OrderCollaborationService orderService;
    private SupplierQualityCollaborationService qualityService;
    private PartnerDocumentExchangeService docService;

    @BeforeEach
    void setup() {
        partnerRepo = new InMemoryPartnerRepo();
        rfqRepo = new InMemoryRfqRepo();
        orderRepo = new InMemoryOrderRepo();
        qualityRepo = new InMemoryQualityRepo();
        docRepo = new InMemoryDocRepo();

        portalService = new SupplierPortalService(partnerRepo);
        rfqService = new RfqCollaborationService(rfqRepo);
        orderService = new OrderCollaborationService(orderRepo);
        qualityService = new SupplierQualityCollaborationService(qualityRepo);
        docService = new PartnerDocumentExchangeService(docRepo);
    }

    @Test
    @DisplayName("Onboard partner through SupplierPortalService")
    void testOnboardPartner() {
        PartnerOrganization p = portalService.onboardPartner(
                workspaceId, "Northvolt AB", PartnerType.SUPPLIER, PartnerTier.TIER_1_DIRECT, "987-654-321", "SE"
        );
        assertThat(p.id()).isNotNull();
        assertThat(portalService.listPartners(workspaceId)).hasSize(1);
    }

    @Test
    @DisplayName("RFQ publishing, quotation submission, and award")
    void testRfqAndQuotation() {
        RfqDocument rfq = rfqService.createRfq(
                workspaceId, "Battery Cell Anode Material", new MaterialId("MAT-ANODE-SYN-01"), 50000.0, LocalDate.now().plusDays(20)
        );

        PartnerOrganizationId sup1 = PartnerOrganizationId.generate();
        PartnerOrganizationId sup2 = PartnerOrganizationId.generate();

        RfqQuotation q1 = rfqService.submitQuotation(workspaceId, rfq.id(), sup1, BigDecimal.valueOf(14.20), 10, 1000.0, LocalDate.now().plusDays(40));
        RfqQuotation q2 = rfqService.submitQuotation(workspaceId, rfq.id(), sup2, BigDecimal.valueOf(13.80), 14, 2000.0, LocalDate.now().plusDays(40));

        RfqDocument awarded = rfqService.awardQuotation(workspaceId, rfq.id(), q2.id());
        assertThat(awarded.status()).isEqualTo(RfqStatus.AWARDED);
        assertThat(awarded.findQuotation(q2.id()).orElseThrow().status()).isEqualTo(QuotationStatus.ACCEPTED);
        assertThat(awarded.findQuotation(q1.id()).orElseThrow().status()).isEqualTo(QuotationStatus.REJECTED);
    }

    @Test
    @DisplayName("Purchase order acknowledgment and ASN dispatch")
    void testOrderCollaboration() {
        PoCollaborationRecord po = orderService.issuePo(
                workspaceId, "PO-8821", PartnerOrganizationId.generate(), new MaterialId("MAT-CELL-CAN"), 10000.0, BigDecimal.valueOf(25000.0), LocalDate.now().plusDays(15)
        );
        assertThat(po.status()).isEqualTo(PoStatus.ISSUED);

        PoCollaborationRecord ack = orderService.acknowledgePo(workspaceId, po.id(), LocalDate.now().plusDays(14));
        assertThat(ack.status()).isEqualTo(PoStatus.ACKNOWLEDGED);

        AdvanceShippingNotice asn = orderService.dispatchAsn(
                workspaceId, po.id(), "TRK-GLOBAL-7788", "Kuehne+Nagel", 10000.0, LocalDate.now().plusDays(3)
        );
        assertThat(asn.trackingNumber()).isEqualTo("TRK-GLOBAL-7788");

        PoCollaborationRecord shippedPo = orderService.getPo(po.id()).orElseThrow();
        assertThat(shippedPo.status()).isEqualTo(PoStatus.SHIPPED);
    }

    @Test
    @DisplayName("Quality 8D incident lifecycle")
    void testQualityCollaboration() {
        QualityIncident8D inc = qualityService.openIncident(
                workspaceId, PartnerOrganizationId.generate(), new MaterialId("MAT-SEPARATOR-PE"), "Micro-puncture in battery separator film", IncidentSeverity.CRITICAL
        );
        assertThat(inc.currentStep()).isEqualTo(EightDStep.D1_TEAM_FORMATION);

        QualityIncident8D step4 = qualityService.advanceStep(workspaceId, inc.id(), EightDStep.D4_ROOT_CAUSE_ANALYSIS);
        assertThat(step4.currentStep()).isEqualTo(EightDStep.D4_ROOT_CAUSE_ANALYSIS);
        assertThat(step4.closed()).isFalse();

        QualityIncident8D closed = qualityService.advanceStep(workspaceId, inc.id(), EightDStep.D8_CONGRATULATE_TEAM);
        assertThat(closed.closed()).isTrue();
    }

    @Test
    @DisplayName("Partner document upload and verification")
    void testPartnerDocumentExchange() {
        PartnerOrganizationId partnerId = PartnerOrganizationId.generate();
        PartnerDocumentRecord doc = docService.uploadDocument(
                workspaceId, partnerId, "ISO-9001-Certificate.pdf", DocumentCategory.QUALITY_CERTIFICATE, "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        assertThat(doc.verified()).isFalse();

        PartnerDocumentRecord verified = docService.verifyDocument(workspaceId, doc.id());
        assertThat(verified.verified()).isTrue();
        assertThat(docService.listPartnerDocuments(workspaceId, partnerId)).hasSize(1);
    }

    // In-Memory Stubs for Ports
    static class InMemoryPartnerRepo implements PartnerOrganizationRepositoryPort {
        private final List<PartnerOrganization> list = new ArrayList<>();
        public void save(PartnerOrganization org) { list.removeIf(p -> p.id().equals(org.id())); list.add(org); }
        public Optional<PartnerOrganization> findById(PartnerOrganizationId id) { return list.stream().filter(p -> p.id().equals(id)).findFirst(); }
        public List<PartnerOrganization> findByWorkspaceId(WorkspaceId ws) { return list.stream().filter(p -> p.workspaceId().equals(ws)).toList(); }
    }

    static class InMemoryRfqRepo implements RfqRepositoryPort {
        private final List<RfqDocument> list = new ArrayList<>();
        public void save(RfqDocument rfq) { list.removeIf(r -> r.id().equals(rfq.id())); list.add(rfq); }
        public Optional<RfqDocument> findById(RfqId id) { return list.stream().filter(r -> r.id().equals(id)).findFirst(); }
        public List<RfqDocument> findByWorkspaceId(WorkspaceId ws) { return list.stream().filter(r -> r.workspaceId().equals(ws)).toList(); }
    }

    static class InMemoryOrderRepo implements OrderCollaborationRepositoryPort {
        private final List<PoCollaborationRecord> pos = new ArrayList<>();
        private final List<AdvanceShippingNotice> asns = new ArrayList<>();
        public void savePo(PoCollaborationRecord po) { pos.removeIf(p -> p.id().equals(po.id())); pos.add(po); }
        public Optional<PoCollaborationRecord> findPoById(PoCollaborationId id) { return pos.stream().filter(p -> p.id().equals(id)).findFirst(); }
        public List<PoCollaborationRecord> findPosByWorkspaceId(WorkspaceId ws) { return pos.stream().filter(p -> p.workspaceId().equals(ws)).toList(); }
        public void saveAsn(AdvanceShippingNotice asn) { asns.removeIf(a -> a.id().equals(asn.id())); asns.add(asn); }
        public Optional<AdvanceShippingNotice> findAsnById(AdvanceShippingNoticeId id) { return asns.stream().filter(a -> a.id().equals(id)).findFirst(); }
        public List<AdvanceShippingNotice> findAsnsByWorkspaceId(WorkspaceId ws) { return asns.stream().filter(a -> a.workspaceId().equals(ws)).toList(); }
    }

    static class InMemoryQualityRepo implements SupplierQualityRepositoryPort {
        private final List<QualityIncident8D> list = new ArrayList<>();
        public void save(QualityIncident8D inc) { list.removeIf(i -> i.id().equals(inc.id())); list.add(inc); }
        public Optional<QualityIncident8D> findById(QualityIncident8DId id) { return list.stream().filter(i -> i.id().equals(id)).findFirst(); }
        public List<QualityIncident8D> findByWorkspaceId(WorkspaceId ws) { return list.stream().filter(i -> i.workspaceId().equals(ws)).toList(); }
    }

    static class InMemoryDocRepo implements PartnerDocumentRepositoryPort {
        private final List<PartnerDocumentRecord> list = new ArrayList<>();
        public void save(PartnerDocumentRecord doc) { list.removeIf(d -> d.id().equals(doc.id())); list.add(doc); }
        public Optional<PartnerDocumentRecord> findById(PartnerDocumentId id) { return list.stream().filter(d -> d.id().equals(id)).findFirst(); }
        public List<PartnerDocumentRecord> findByWorkspaceId(WorkspaceId ws) { return list.stream().filter(d -> d.workspaceId().equals(ws)).toList(); }
        public List<PartnerDocumentRecord> findByPartnerId(WorkspaceId ws, PartnerOrganizationId partnerId) {
            return list.stream().filter(d -> d.workspaceId().equals(ws) && d.partnerId().equals(partnerId)).toList();
        }
    }
}

