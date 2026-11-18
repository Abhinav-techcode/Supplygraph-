package com.supplygraph.application.outbox;

import com.supplygraph.application.port.out.InboxRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class InboxServiceTest {
    private final Set<String> seen = new HashSet<>();
    private final InboxRepositoryPort port = (ws, consumer, id, now) -> seen.add(ws.value() + consumer + id);
    private final InboxService inbox = new InboxService(port);

    @Test void effectRunsOnceForDuplicateDelivery() {
        AtomicInteger effects = new AtomicInteger();
        WorkspaceId ws = WorkspaceId.of("ws");
        assertThat(inbox.processOnce(ws, "c", "m1", effects::incrementAndGet)).isTrue();
        assertThat(inbox.processOnce(ws, "c", "m1", effects::incrementAndGet)).isFalse();
        assertThat(inbox.processOnce(ws, "c", "m2", effects::incrementAndGet)).isTrue();
        assertThat(effects.get()).isEqualTo(2);
    }

    @Test void sameMessageInDifferentWorkspaceIsIndependent() {
        AtomicInteger effects = new AtomicInteger();
        inbox.processOnce(WorkspaceId.of("a"), "c", "m", effects::incrementAndGet);
        inbox.processOnce(WorkspaceId.of("b"), "c", "m", effects::incrementAndGet);
        assertThat(effects.get()).isEqualTo(2);
    }
}
