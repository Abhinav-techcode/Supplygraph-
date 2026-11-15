package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "inventory_balances")
public class InventoryBalanceJpaEntity {

    @Id
    @Column(name = "id", length = 128, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "node_id", length = 64, nullable = false)
    private String nodeId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "category", length = 64, nullable = false)
    private String category;

    @Column(name = "lot_number", length = 128)
    private String lotNumber;

    @Column(name = "balance_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal balanceAmount;

    @Column(name = "unit", length = 32, nullable = false)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryBalanceJpaEntity() {
    }

    public InventoryBalanceJpaEntity(String id, String workspaceId, String nodeId, String materialId,
                                    String category, String lotNumber, BigDecimal balanceAmount,
                                    String unit, Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.nodeId = nodeId;
        this.materialId = materialId;
        this.category = category;
        this.lotNumber = lotNumber;
        this.balanceAmount = balanceAmount;
        this.unit = unit;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getCategory() {
        return category;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

