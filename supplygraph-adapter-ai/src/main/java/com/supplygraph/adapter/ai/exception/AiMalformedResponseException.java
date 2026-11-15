package com.supplygraph.adapter.ai.exception;

public class AiMalformedResponseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AiMalformedResponseException(String message) {
        super(message);
    }
}
