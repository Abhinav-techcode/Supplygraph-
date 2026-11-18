package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record RfqQuotation(
        RfqQuotationId id,
        RfqId rfqId,
        PartnerOrganizationId supplierId,
        BigDecimal unitPriceUsd,
        int leadTimeDays,
        double minOrderQuantity,
        LocalDate validityDate,
        QuotationStatus status,
        Instant submittedAt
) implements Serializable {
    public RfqQuotation {
        Objects.requireNonNull(id, "Quotation ID cannot be null");
        Objects.requireNonNull(rfqId, "RFQ ID cannot be null");
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(unitPriceUsd, "Unit price cannot be null");
        Objects.requireNonNull(validityDate, "Validity date cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(submittedAt, "Submitted timestamp cannot be null");
        if (unitPriceUsd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (leadTimeDays < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative");
        }
        if (minOrderQuantity < 0) {
            throw new IllegalArgumentException("Minimum order quantity cannot be negative");
        }
    }

    public RfqQuotation withStatus(QuotationStatus newStatus) {
        return new RfqQuotation(id, rfqId, supplierId, unitPriceUsd, leadTimeDays, minOrderQuantity, validityDate, newStatus, submittedAt);
    }
}

