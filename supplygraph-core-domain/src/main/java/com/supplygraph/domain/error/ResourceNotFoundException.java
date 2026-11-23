package com.supplygraph.domain.error;

/** A referenced item does not exist (or is not visible to the caller). */
public class ResourceNotFoundException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public ResourceNotFoundException(String message) { super(ErrorCode.NOT_FOUND, message); }
    public ResourceNotFoundException(String type, Object id) { super(ErrorCode.NOT_FOUND, type + " not found: " + id); with("type", type).with("id", String.valueOf(id)); }
}
