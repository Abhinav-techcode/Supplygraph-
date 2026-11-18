package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;

public interface InboxRepositoryPort {
    /** @return true if this (workspace, consumer, messageId) was newly recorded; false if it was already processed. */
    boolean registerIfAbsent(WorkspaceId ws, String consumer, String messageId, Instant now);
}
