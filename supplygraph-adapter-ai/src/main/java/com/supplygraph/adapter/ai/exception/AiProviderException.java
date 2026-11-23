package com.supplygraph.adapter.ai.exception;

import com.supplygraph.domain.error.ExternalServiceException;

/** AI provider failed. Raised by the AI client boundary; callers fall back to deterministic behaviour or surface a clear message. */
public class AiProviderException extends ExternalServiceException {
    private static final long serialVersionUID = 1L;
    public AiProviderException(String message) { super("ai-provider", message, true, null); }
}
