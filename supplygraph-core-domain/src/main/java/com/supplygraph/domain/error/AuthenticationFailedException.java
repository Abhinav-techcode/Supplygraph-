package com.supplygraph.domain.error;

/** No valid sign-in for an action that needs one. */
public class AuthenticationFailedException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public AuthenticationFailedException(String message) { super(ErrorCode.AUTHENTICATION_FAILED, message); }
}
