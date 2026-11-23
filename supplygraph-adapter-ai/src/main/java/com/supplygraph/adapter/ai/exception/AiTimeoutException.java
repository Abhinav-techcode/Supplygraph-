package com.supplygraph.adapter.ai.exception;

import com.supplygraph.domain.error.ExternalServiceException;

/** AI provider timed out. Raised by the AI client boundary; callers fall back to deterministic behaviour or surface a clear message. */
public class AiTimeoutException extends ExternalServiceException {
    private static final long serialVersionUID = 1L;
    public AiTimeoutException(String message) { super("ai-provider", message, true, null); }
}
