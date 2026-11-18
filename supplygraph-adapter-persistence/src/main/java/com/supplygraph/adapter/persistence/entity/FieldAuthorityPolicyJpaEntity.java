package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;

@Entity @Table(name = "field_authority_policies")
public class FieldAuthorityPolicyJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(name = "entity_type", length = 64, nullable = false) private String entityType;
    @Column(name = "field_name", length = 64, nullable = false) private String fieldName;
    @Column(length = 16, nullable = false) private String authority;
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; }
    public String getFieldName() { return fieldName; } public void setFieldName(String v) { fieldName = v; }
    public String getAuthority() { return authority; } public void setAuthority(String v) { authority = v; }
}
