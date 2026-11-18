package com.supplygraph.application.outbox;

import com.supplygraph.application.port.out.InboxRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Inbox deduplication: the business effect and the inbox record commit atomically, so redelivery has no second effect. */
@Service
public class InboxService {
    private final InboxRepositoryPort inbox;

    public InboxService(InboxRepositoryPort inbox) { this.inbox = inbox; }

    /** @return true if the effect ran; false if the message had already been processed. */
    @Transactional
    public boolean processOnce(WorkspaceId ws, String consumer, String messageId, Runnable effect) {
        if (!inbox.registerIfAbsent(ws, consumer, messageId, Instant.now())) {
            return false;
        }
        effect.run(); // a failure rolls back the inbox row too, so a retry is not wrongly suppressed
        return true;
    }
}
