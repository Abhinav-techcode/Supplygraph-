package com.supplygraph.application.integration;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.ImportJobRepositoryPort;
import com.supplygraph.application.provenance.DataProvenanceService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.integration.*;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.domain.model.security.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
            throw new IllegalArgumentException("File size exceeds maximum permitted threshold of 10MB (actual: " + fileBytes.length + " bytes)");
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
                .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + jobId));

        if (!job.workspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Job workspace mismatch: " + job.workspaceId() + " != " + workspaceId);
        }

        if (job.status() != ImportStatus.VALIDATED) {
            throw new IllegalStateException("Import job cannot be executed in status: " + job.status());
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
