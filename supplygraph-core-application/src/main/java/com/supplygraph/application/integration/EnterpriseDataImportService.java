package com.supplygraph.application.integration;

import com.supplygraph.domain.error.InvalidStateException;
import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.ImportJobRepositoryPort;
import com.supplygraph.application.provenance.DataProvenanceService;
import com.supplygraph.application.sample.SampleSupplyChainFactory;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.integration.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the complete 18-step enterprise data integration, validation, deduplication, and ingestion workflow.
 */
@Service
public class EnterpriseDataImportService {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseDataImportService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final ImportJobRepositoryPort importJobRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final DataProvenanceService provenanceService;
    private final AuditGovernanceService auditGovernanceService;

    private final CsvDataParser csvParser = new CsvDataParser();
    private final JsonDataParser jsonParser = new JsonDataParser();

    // In-memory staging of parsed records for validated jobs pending user execution confirmation
    private final Map<ImportJobId, List<RawImportRecord>> stagedRecords = new ConcurrentHashMap<>();

    public EnterpriseDataImportService(
            ImportJobRepositoryPort importJobRepository,
            DigitalTwinRepositoryPort digitalTwinRepository,
            DataProvenanceService provenanceService,
            AuditGovernanceService auditGovernanceService
    ) {
        this.importJobRepository = importJobRepository;
        this.digitalTwinRepository = digitalTwinRepository;
        this.provenanceService = provenanceService;
        this.auditGovernanceService = auditGovernanceService;
    }

    /**
     * Parses, validates, and stages incoming dataset, producing a validation preview without mutating digital twin state.
     */
    public ImportJob parseAndValidate(
            byte[] fileBytes,
            ImportSourceType sourceType,
            DataCategory category,
            String filename,
            String workspaceId,
            String userId
    ) throws IOException {
        Objects.requireNonNull(fileBytes, "File bytes cannot be null");
        Objects.requireNonNull(sourceType, "ImportSourceType cannot be null");
        Objects.requireNonNull(category, "DataCategory cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");

        if (fileBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("File size exceeds maximum permitted threshold of 10MB (actual: " + fileBytes.length + " bytes)");
        }

        String checksum = computeSha256(fileBytes);
        ImportJob job = ImportJob.createNew(workspaceId, sourceType, category, filename, checksum, userId != null ? userId : "OPERATOR");

        List<RawImportRecord> parsedRecords = new ArrayList<>();
        List<ImportRecordFailure> failures = new ArrayList<>();

        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            if (sourceType == ImportSourceType.CSV) {
                CsvDataParser.ParseResult result = csvParser.parse(is, category);
                parsedRecords.addAll(result.records());
                failures.addAll(result.failures());
            } else if (sourceType == ImportSourceType.JSON) {
                JsonDataParser.ParseResult result = jsonParser.parse(is, category);
                parsedRecords.addAll(result.records());
                failures.addAll(result.failures());
            } else {
                failures.add(new ImportRecordFailure(0, "SOURCE_TYPE", sourceType.name(), "UNSUPPORTED_FORMAT", "Automated parsing only supports CSV and JSON", "Select CSV or JSON upload"));
            }
        }

        // Domain validation & duplicate detection
        Set<String> observedKeys = new HashSet<>();
        List<RawImportRecord> validRecords = new ArrayList<>();

        for (RawImportRecord record : parsedRecords) {
            if (!record.valid()) {
                continue;
            }

            List<ImportRecordFailure> recordErrors = validateRecordDomainRules(record, category, observedKeys);
            if (recordErrors.isEmpty()) {
                validRecords.add(record);
            } else {
                failures.addAll(recordErrors);
            }
        }

        int total = parsedRecords.size();
        int accepted = validRecords.size();
        int rejected = total - accepted;

        job.markValidated(total, accepted, rejected, failures);
        stagedRecords.put(job.id(), validRecords);
        importJobRepository.save(job);

        log.info("Import job {} validated: {} total, {} accepted, {} rejected (failures: {})",
                job.id(), total, accepted, rejected, failures.size());
        return job;
    }

    /**
     * Executes an authorized transactional import of previously validated and staged records.
     */
    public ImportJob executeImport(ImportJobId jobId, String workspaceId, String userId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ValidationException("Import job not found: " + jobId));

        if (!job.workspaceId().equals(workspaceId)) {
            throw new ValidationException("Job workspace mismatch: " + job.workspaceId() + " != " + workspaceId);
        }

        if (job.status() != ImportStatus.VALIDATED) {
            throw new InvalidStateException("Import job cannot be executed in status: " + job.status());
        }

        List<RawImportRecord> records = stagedRecords.getOrDefault(jobId, Collections.emptyList());
        job.markInProgress();
        importJobRepository.save(job);

        int executedCount = 0;
        int failedCount = 0;

        for (RawImportRecord record : records) {
            try {
                String primaryKey = extractPrimaryKey(record, job.category());
                provenanceService.registerProvenance(
                        WorkspaceId.of(workspaceId),
                        primaryKey,
                        job.category().name(),
                        ProvenanceType.REAL_IMPORTED,
                        job.filename() + " (Job: " + jobId.value() + ")",
                        "v1",
                        record.fields()
                );
                executedCount++;
            } catch (Exception ex) {
                log.warn("Failed to ingest record at row {}: {}", record.rowIndex(), ex.getMessage());
                failedCount++;
            }
        }

        job.markCompleted(executedCount, failedCount);
        importJobRepository.save(job);
        stagedRecords.remove(jobId);

        // Mutate and persist the updated digital twin state
        if (executedCount > 0) {
            try {
                applyRecordsToTwin(WorkspaceId.of(workspaceId), job.category(), records);
            } catch (Exception ex) {
                log.warn("Failed to apply imported records to digital twin for workspace {}: {}", workspaceId, ex.getMessage());
            }
        }

        auditGovernanceService.logAction(
                UserId.of(userId != null ? userId : "OPERATOR"),
                WorkspaceId.of(workspaceId),
                com.supplygraph.domain.model.audit.AuditAction.DATA_IMPORTED,
                jobId.value(),
                "v0",
                "v1",
                "Imported " + executedCount + " records (" + job.category() + ") from " + job.filename(),
                "CONF-NONE"
        );

        log.info("Import job {} execution complete: {} imported, {} failed", jobId, executedCount, failedCount);
        return job;
    }

    /**
     * Explicit user action to initialize or reset the workspace digital twin to the canonical
     * realistic multi-tier sample scenario.
     */
    public DigitalTwinState loadSampleSupplyChain(String workspaceId, String userId) {
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        WorkspaceId wsId = WorkspaceId.of(workspaceId);
        DigitalTwinState sample = SampleSupplyChainFactory.createSampleTwin(wsId);
        digitalTwinRepository.save(sample);

        auditGovernanceService.logAction(
                UserId.of(userId != null ? userId : "OPERATOR"),
                wsId,
                com.supplygraph.domain.model.audit.AuditAction.DATA_IMPORTED,
                "DIGITAL_TWIN:" + workspaceId,
                "v0",
                "v" + sample.getCurrentVersion().value(),
                "Loaded canonical sample supply chain network (18 facilities, 18 lanes, multi-tier BOM)",
                "CONF-NONE"
        );
        log.info("Loaded sample supply chain for workspace {} by user {}", workspaceId, userId);
        return sample;
    }

    private void applyRecordsToTwin(WorkspaceId wsId, DataCategory category, List<RawImportRecord> records) {
        DigitalTwinState current = digitalTwinRepository.findByWorkspaceId(wsId)
                .orElseGet(() -> new DigitalTwinState(wsId, StateVersion.of(1L), new SupplyChainGraph(List.of(), List.of()), new BillOfMaterials(List.of(), List.of())));

        SupplyChainGraph graph = current.getGraph();
        BillOfMaterials bom = current.getBom();
        Map<NodeId, SupplyChainNode> nodes = new LinkedHashMap<>();
        if (graph != null) {
            graph.getAllNodes().forEach(n -> nodes.put(n.getId(), n));
        }
        Map<EdgeId, TransportationLane> lanes = new LinkedHashMap<>();
        if (graph != null) {
            graph.getAllLanes().forEach(l -> lanes.put(l.getId(), l));
        }
        Map<MaterialId, Material> materials = new LinkedHashMap<>();
        if (bom != null) {
            bom.getAllMaterials().forEach(m -> materials.put(m.getId(), m));
        }
        List<BomEntry> bomEntries = bom != null ? new ArrayList<>(bom.getAllEntries()) : new ArrayList<>();

        for (RawImportRecord r : records) {
            Map<String, String> f = r.fields();
            switch (category) {
                case SUPPLIER -> {
                    String id = f.getOrDefault("supplier_id", f.get("id"));
                    if (id != null && !id.isBlank()) {
                        String name = f.getOrDefault("name", id);
                        String loc = f.getOrDefault("location", "GLOBAL");
                        int tier = Integer.parseInt(f.getOrDefault("tier", "1"));
                        double risk = Double.parseDouble(f.getOrDefault("risk_score", "0.1"));
                        nodes.put(NodeId.of(id), new Supplier(NodeId.of(id), name, loc, true, tier, Set.of("ISO9001"), risk));
                    }
                }
                case LOGISTICS -> {
                    String id = f.getOrDefault("lane_id", f.get("id"));
                    String src = f.get("source_node_id");
                    String tgt = f.get("target_node_id");
                    if (id != null && src != null && tgt != null && nodes.containsKey(NodeId.of(src)) && nodes.containsKey(NodeId.of(tgt))) {
                        TransportMode mode = TransportMode.valueOf(f.getOrDefault("transport_mode", "ROAD").toUpperCase());
                        long hours = Long.parseLong(f.getOrDefault("transit_hours", "24"));
                        BigDecimal cost = new BigDecimal(f.getOrDefault("cost_per_kg", "1.50"));
                        lanes.put(EdgeId.of(id), new TransportationLane(EdgeId.of(id), NodeId.of(src), NodeId.of(tgt), mode, Duration.ofHours(hours), cost, true));
                    }
                }
                case MATERIAL -> {
                    String id = f.getOrDefault("material_id", f.get("id"));
                    if (id != null && !id.isBlank()) {
                        String sku = f.getOrDefault("sku", id);
                        String name = f.getOrDefault("name", id);
                        int leadDays = Integer.parseInt(f.getOrDefault("lead_time_days", "14"));
                        materials.put(MaterialId.of(id), new Component(MaterialId.of(id), sku, name, UnitOfMeasure.PIECE, leadDays));
                    }
                }
                case INVENTORY -> {
                    String nodeId = f.get("node_id");
                    String matId = f.get("material_id");
                    String amountStr = f.getOrDefault("balance_amount", f.get("quantity"));
                    if (nodeId != null && matId != null && amountStr != null) {
                        BigDecimal amt = new BigDecimal(amountStr);
                        InventoryAccountId acc = InventoryAccountId.of(NodeId.of(nodeId), MaterialId.of(matId), BalanceCategory.ON_HAND_AVAILABLE);
                        InventoryAccountId off = InventoryAccountId.of(NodeId.of(nodeId), MaterialId.of(matId), BalanceCategory.EXTERNAL_OFFSET);
                        Quantity q = Quantity.of(amt, UnitOfMeasure.PIECE);
                        current.getInventoryLedger().postTransaction(new InventoryTransaction(
                                TransactionId.generate(), Instant.now(), "IMPORT_BALANCE", "JOB",
                                List.of(new InventoryPosting(UUID.randomUUID().toString(), acc, PostingType.DEBIT, q),
                                        new InventoryPosting(UUID.randomUUID().toString(), off, PostingType.CREDIT, q))
                        ));
                    }
                }
                default -> {}
            }
        }

        SupplyChainGraph updatedGraph = new SupplyChainGraph(new ArrayList<>(nodes.values()), new ArrayList<>(lanes.values()));
        BillOfMaterials updatedBom = new BillOfMaterials(new ArrayList<>(materials.values()), bomEntries);
        DigitalTwinState updatedState = new DigitalTwinState(wsId, current.getCurrentVersion().next(), updatedGraph, updatedBom);
        current.getInventoryLedger().getTransactionHistory().forEach(tx -> updatedState.getInventoryLedger().postTransaction(tx));
        digitalTwinRepository.save(updatedState);
    }

    public Optional<ImportJob> getImportJob(ImportJobId id) {
        return importJobRepository.findById(id);
    }

    public List<ImportJob> listImportJobs(String workspaceId) {
        return importJobRepository.findByWorkspaceId(workspaceId);
    }

    private List<ImportRecordFailure> validateRecordDomainRules(
            RawImportRecord record,
            DataCategory category,
            Set<String> observedKeys
    ) {
        List<ImportRecordFailure> errors = new ArrayList<>();
        Map<String, String> f = record.fields();
        int row = record.rowIndex();

        switch (category) {
            case SUPPLIER -> {
                String id = f.getOrDefault("supplier_id", f.get("id"));
                if (id == null || id.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "supplier_id", id, "MISSING_REQUIRED_FIELD", "Supplier ID is required", "Provide unique supplier ID"));
                } else if (!observedKeys.add("SUPPLIER:" + id.toUpperCase())) {
                    errors.add(new ImportRecordFailure(row, "supplier_id", id, "DUPLICATE_KEY", "Duplicate supplier ID in batch", "Ensure supplier ID is unique"));
                }

                String leadTimeStr = f.get("lead_time");
                if (leadTimeStr != null && !leadTimeStr.isBlank()) {
                    try {
                        int lt = Integer.parseInt(leadTimeStr);
                        if (lt < 0) {
                            errors.add(new ImportRecordFailure(row, "lead_time", leadTimeStr, "NEGATIVE_VALUE", "Lead time cannot be negative", "Set lead time >= 0 days"));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new ImportRecordFailure(row, "lead_time", leadTimeStr, "INVALID_NUMBER", "Lead time must be an integer", "Specify numeric days"));
                    }
                }
            }
            case MATERIAL -> {
                String id = f.getOrDefault("material_id", f.get("id"));
                if (id == null || id.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "material_id", id, "MISSING_REQUIRED_FIELD", "Material ID is required", "Provide unique material ID"));
                } else if (!observedKeys.add("MATERIAL:" + id.toUpperCase())) {
                    errors.add(new ImportRecordFailure(row, "material_id", id, "DUPLICATE_KEY", "Duplicate material ID in batch", "Ensure material ID is unique"));
                }

                String uom = f.get("unit");
                if (uom != null && !uom.isBlank()) {
                    String norm = uom.toUpperCase();
                    if (!List.of("PIECE", "PIECES", "KG", "GRAM", "LITRE", "LITER", "HOUR", "HOURS", "METER", "METERS").contains(norm)) {
                        errors.add(new ImportRecordFailure(row, "unit", uom, "INVALID_UOM", "Unrecognized unit of measure", "Use standard units (PIECE, KG, LITRE, HOUR)"));
                    }
                }
            }
            case INVENTORY -> {
                String wh = f.get("warehouse_id");
                String mat = f.get("material_id");
                String qtyStr = f.get("quantity");

                if (wh == null || wh.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "warehouse_id", wh, "MISSING_REQUIRED_FIELD", "Warehouse ID is required", "Specify destination warehouse"));
                }
                if (mat == null || mat.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "material_id", mat, "MISSING_REQUIRED_FIELD", "Material ID is required", "Specify inventory material ID"));
                }
                if (qtyStr == null || qtyStr.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "quantity", qtyStr, "MISSING_REQUIRED_FIELD", "Quantity is required", "Specify stock quantity"));
                } else {
                    try {
                        BigDecimal qty = new BigDecimal(qtyStr);
                        if (qty.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(new ImportRecordFailure(row, "quantity", qtyStr, "NEGATIVE_QUANTITY", "Inventory quantity cannot be negative", "Provide quantity >= 0"));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new ImportRecordFailure(row, "quantity", qtyStr, "INVALID_DECIMAL", "Quantity must be numeric", "Enter valid decimal number"));
                    }
                }
            }
            case PRODUCTION -> {
                String site = f.getOrDefault("site_id", f.get("manufacturing_site_id"));
                String line = f.get("line_id");
                if (site == null || site.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "site_id", site, "MISSING_REQUIRED_FIELD", "Manufacturing site ID is required", "Specify site node ID"));
                }
                if (line == null || line.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "line_id", line, "MISSING_REQUIRED_FIELD", "Production line ID is required", "Specify line identifier"));
                }
            }
            case LOGISTICS -> {
                String origin = f.get("origin_id");
                String dest = f.get("destination_id");
                if (origin == null || origin.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "origin_id", origin, "MISSING_REQUIRED_FIELD", "Origin node ID is required", "Specify origin facility"));
                }
                if (dest == null || dest.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "destination_id", dest, "MISSING_REQUIRED_FIELD", "Destination node ID is required", "Specify destination facility"));
                }
            }
            case DEMAND -> {
                String demId = f.getOrDefault("demand_id", f.get("customer_id"));
                String prodId = f.get("product_id");
                String qtyStr = f.get("quantity");

                if (demId == null || demId.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "demand_id", demId, "MISSING_REQUIRED_FIELD", "Demand or customer identifier is required", "Provide customer order ID"));
                }
                if (prodId == null || prodId.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "product_id", prodId, "MISSING_REQUIRED_FIELD", "Product ID is required", "Specify demanded product ID"));
                }
                if (qtyStr == null || qtyStr.isBlank()) {
                    errors.add(new ImportRecordFailure(row, "quantity", qtyStr, "MISSING_REQUIRED_FIELD", "Quantity is required", "Specify order quantity"));
                }
            }
        }
        return errors;
    }

    private String extractPrimaryKey(RawImportRecord record, DataCategory category) {
        Map<String, String> f = record.fields();
        return switch (category) {
            case SUPPLIER -> f.getOrDefault("supplier_id", f.getOrDefault("id", "SUP-" + record.rowIndex()));
            case MATERIAL -> f.getOrDefault("material_id", f.getOrDefault("id", "MAT-" + record.rowIndex()));
            case INVENTORY -> f.getOrDefault("warehouse_id", "WH") + ":" + f.getOrDefault("material_id", "MAT") + ":" + f.getOrDefault("lot_or_batch", "LOT-0");
            case PRODUCTION -> f.getOrDefault("site_id", "SITE") + ":" + f.getOrDefault("line_id", "LINE-" + record.rowIndex());
            case LOGISTICS -> f.getOrDefault("lane_id", f.getOrDefault("origin_id", "ORIGIN") + "->" + f.getOrDefault("destination_id", "DEST"));
            case DEMAND -> f.getOrDefault("demand_id", f.getOrDefault("customer_id", "DEM-" + record.rowIndex()));
        };
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "CHECKSUM_FAILED";
        }
    }
}
