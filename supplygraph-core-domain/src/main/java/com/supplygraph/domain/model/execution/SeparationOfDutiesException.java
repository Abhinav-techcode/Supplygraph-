package com.supplygraph.domain.model.execution;

/** A governance rule forbids this user from performing the action (e.g. approving their own workflow). */
public class SeparationOfDutiesException extends RuntimeException {
    public SeparationOfDutiesException(String message) { super(message); }
}
