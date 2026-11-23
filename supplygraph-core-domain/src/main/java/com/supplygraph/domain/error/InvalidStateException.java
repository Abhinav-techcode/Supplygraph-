package com.supplygraph.domain.error;

/** The action is not allowed in the item's current state (for example starting a workflow that is still blocked). */
public class InvalidStateException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public InvalidStateException(String message) { super(ErrorCode.INVALID_STATE, message); }
    public InvalidStateException(String what, Object currentState) { super(ErrorCode.INVALID_STATE, what + " (current state: " + currentState + ")"); with("state", String.valueOf(currentState)); }
}
