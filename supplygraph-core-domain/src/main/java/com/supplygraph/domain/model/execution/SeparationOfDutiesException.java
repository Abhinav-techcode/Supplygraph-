package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.error.BusinessRuleException;
/** A governance rule forbids this user from performing the action (e.g. approving their own workflow). */
public class SeparationOfDutiesException extends BusinessRuleException {
    public SeparationOfDutiesException(String message) { super(message); }
}
