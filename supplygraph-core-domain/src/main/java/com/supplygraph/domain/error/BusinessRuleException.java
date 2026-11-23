package com.supplygraph.domain.error;

/** The request is well-formed but a business rule forbids it (for example separation of duties, or no feasible recovery option). */
public class BusinessRuleException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public BusinessRuleException(String message) { super(ErrorCode.BUSINESS_RULE_VIOLATION, message); }
    protected BusinessRuleException(ErrorCode code, String message) { super(code, message); }
}
