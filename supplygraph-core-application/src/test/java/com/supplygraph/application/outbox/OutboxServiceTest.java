package com.supplygraph.application.outbox;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws-1");
    private OutboxRepositoryPort repo;
    private AuditGovernanceService audit;

    @BeforeEach void setUp() {
        repo = mock(OutboxRepositoryPort.class);
        audit = mock(AuditGovernanceService.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @AfterEach void tearDown() { SecurityContext.clear(); }

    private OutboxService service(OutboxHandler... hs) { return new OutboxService(repo, new WorkspaceAccessGuard(), audit, List.of(hs)); }
    private OutboxEvent claimed() { return OutboxEvent.create(ws, "T", "a", "{}", "k", "corr").markInFlight(Instant.now()); }
    private static OutboxHandler handler(String type, Runnable r) {
        return new OutboxHandler() {
            public String eventType() { return type; }
            public void handle(OutboxEvent e) { r.run(); }
        };
    }
    private void login(UserRole role, WorkspaceId... wss) {
        SecurityContext.setCurrentUser(UserProfile.create("U1", "u", "u@x.io", "U", role, Set.of(wss)));
    }

    @Test void deliversClaimedEvents() {
        when(repo.claimDue(any(), anyInt())).thenReturn(List.of(claimed()));
        assertThat(service(handler("T", () -> {})).dispatchBatch(10)).isEqualTo(1);
        ArgumentCaptor<OutboxEvent> c = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(c.capture());
        assertThat(c.getValue().status()).isEqualTo(OutboxStatus.DELIVERED);
    }

    @Test void retryableFailureIsRescheduledNotLost() {
        when(repo.claimDue(any(), anyInt())).thenReturn(List.of(claimed()));
        OutboxHandler h = handler("T", () -> { throw new DeliveryException("timeout", true); });
        assertThat(service(h).dispatchBatch(10)).isZero();
        ArgumentCaptor<OutboxEvent> c = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(c.capture());
        assertThat(c.getValue().status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(c.getValue().nextAttemptAt()).isAfter(Instant.now());
    }

    @Test void permanentFailureAndUnknownTypeDeadLetter() {
        when(repo.claimDue(any(), anyInt())).thenReturn(List.of(claimed()));
        service(handler("OTHER", () -> {})).dispatchBatch(10);
        ArgumentCaptor<OutboxEvent> c = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(c.capture());
        assertThat(c.getValue().status()).isEqualTo(OutboxStatus.DEAD_LETTER);
    }

    @Test void unexpectedRuntimeExceptionIsContainedAndRetried() {
        when(repo.claimDue(any(), anyInt())).thenReturn(List.of(claimed(), claimed()));
        OutboxHandler h = handler("T", () -> { throw new IllegalStateException("npe"); });
        assertThat(service(h).dispatchBatch(10)).isZero();
        verify(repo, times(2)).save(any());
    }

    @Test void duplicateHandlerRegistrationRejected() {
        assertThatThrownBy(() -> service(handler("T", () -> {}), handler("T", () -> {}))).isInstanceOf(ConflictException.class);
    }

    @Test void replayRequiresPermissionInEventWorkspaceAndRationale() {
        OutboxEvent dead = claimed().markFailed("x", false, new com.supplygraph.domain.model.outbox.BackoffPolicy(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(2), 1), Instant.now());
        when(repo.findById(dead.id())).thenReturn(Optional.of(dead));
        OutboxService s = service();

        assertThatThrownBy(() -> s.replay(dead.id(), "why")).isInstanceOf(WorkspaceAccessDeniedException.class); // not logged in
        login(UserRole.ANALYST, ws);
        assertThatThrownBy(() -> s.replay(dead.id(), "why")).isInstanceOf(UnauthorizedOperationException.class);
        login(UserRole.SUPPLY_CHAIN_MANAGER, WorkspaceId.of("other"));
        assertThatThrownBy(() -> s.replay(dead.id(), "why")).isInstanceOf(WorkspaceAccessDeniedException.class);
        login(UserRole.SUPPLY_CHAIN_MANAGER, ws);
        assertThatThrownBy(() -> s.replay(dead.id(), " ")).isInstanceOf(ValidationException.class);
        verify(repo, never()).save(any());

        assertThat(s.replay(dead.id(), "endpoint fixed").status()).isEqualTo(OutboxStatus.PENDING);
        verify(audit).logAction(any(), eq(ws), any(), eq(dead.id()), any(), any(), eq("endpoint fixed"), any());
    }
}
