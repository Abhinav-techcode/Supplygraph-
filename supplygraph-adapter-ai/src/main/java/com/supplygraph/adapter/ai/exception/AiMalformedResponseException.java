package com.supplygraph.adapter.ai.exception;

import com.supplygraph.domain.error.ExternalServiceException;

/** AI provider returned a malformed response. Raised by the AI client boundary; callers fall back to deterministic behaviour or surface a clear message. */
public class AiMalformedResponseException extends ExternalServiceException {
    private static final long serialVersionUID = 1L;
    public AiMalformedResponseException(String message) { super("ai-provider", message, false, null); }
}
