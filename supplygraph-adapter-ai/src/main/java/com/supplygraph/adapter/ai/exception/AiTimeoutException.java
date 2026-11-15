package com.supplygraph.adapter.ai.exception;

public class AiTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AiTimeoutException(String message) {
        super(message);
    }
}
