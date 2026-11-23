package com.supplygraph.application.security;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.port.out.UserCredentialRepositoryPort;
import com.supplygraph.application.port.out.UserProfileRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Authentication: hashing, uniform failures, lockout")
class AuthenticationServiceTest {
    private final Map<String, UserProfile> profiles = new HashMap<>();
    private final Map<String, UserCredentialRepositoryPort.Credential> creds = new HashMap<>();
    private Clock clock;
    private AuthenticationService svc;
    private final WorkspaceId ws = WorkspaceId.of("WS-AUTO-01");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        UserProfileRepositoryPort users = new UserProfileRepositoryPort() {
            public Optional<UserProfile> findById(UserId id) { return profiles.values().stream().filter(p -> p.id().equals(id)).findFirst(); }
            public Optional<UserProfile> findByUsername(String u) { return Optional.ofNullable(profiles.get(u)); }
            public List<UserProfile> findAll() { return new ArrayList<>(profiles.values()); }
            public UserProfile save(UserProfile p) { profiles.put(p.username(), p); return p; }
            public void deleteById(UserId id) {}
        };
        UserCredentialRepositoryPort store = new UserCredentialRepositoryPort() {
            public Optional<Credential> find(String id) { return Optional.ofNullable(creds.get(id)); }
            public void savePassword(String id, String h) { creds.put(id, new Credential(id, h, 0, null)); }
            public void recordFailure(String id, int n, Instant until) { var c = creds.get(id); creds.put(id, new Credential(id, c.passwordHash(), n, until)); }
            public void clearFailures(String id) { var c = creds.get(id); creds.put(id, new Credential(id, c.passwordHash(), 0, null)); }
        };
        svc = new AuthenticationService(users, store, clock);
        svc.register("Manager", "Mia Manager", "mia@example.com", UserRole.SUPPLY_CHAIN_MANAGER, "correct-horse-1", ws);
    }

    @Test
    @DisplayName("Correct credentials succeed (username is case-insensitive); the password is never stored")
    void succeeds() {
        var r = svc.authenticate("MANAGER", "correct-horse-1");
        assertThat(r.success()).isTrue();
        assertThat(r.user().role()).isEqualTo(UserRole.SUPPLY_CHAIN_MANAGER);
        assertThat(creds.values().iterator().next().passwordHash()).startsWith("pbkdf2$").doesNotContain("correct-horse-1");
    }

    @Test
    @DisplayName("Wrong password, unknown user and blank input all give the same message")
    void uniformFailure() {
        String m1 = svc.authenticate("manager", "nope-nope-1").message();
        String m2 = svc.authenticate("ghost", "whatever-1").message();
        String m3 = svc.authenticate("", "").message();
        assertThat(m1).isEqualTo(m2).isEqualTo(m3).isEqualTo("Wrong username or password.");
    }

    @Test
    @DisplayName("Five failures lock the account, even for the right password, until the lock expires")
    void locksAfterRepeatedFailures() {
        for (int i = 0; i < AuthenticationService.MAX_FAILURES; i++) svc.authenticate("manager", "bad-password-" + i);
        var locked = svc.authenticate("manager", "correct-horse-1");
        assertThat(locked.success()).isFalse();
        assertThat(locked.message()).contains("Too many failed attempts");

        Clock later = Clock.fixed(clock.instant().plus(AuthenticationService.LOCK).plusSeconds(1), ZoneOffset.UTC);
        var users = new AuthenticationService(svcUsers(), svcStore(), later);
        assertThat(users.authenticate("manager", "correct-horse-1").success()).isTrue();
    }

    @Test
    @DisplayName("A success clears earlier failures")
    void successResetsCounter() {
        svc.authenticate("manager", "bad-password-1");
        svc.authenticate("manager", "bad-password-2");
        assertThat(svc.authenticate("manager", "correct-horse-1").success()).isTrue();
        assertThat(creds.values().iterator().next().failedAttempts()).isZero();
    }

    @Test
    @DisplayName("Inactive users cannot sign in")
    void inactiveRejected() {
        UserProfile p = profiles.get("manager");
        profiles.put("manager", new UserProfile(p.id(), p.username(), p.email(), p.displayName(), p.role(), p.assignedWorkspaces(), false, p.createdAt()));
        assertThat(svc.authenticate("manager", "correct-horse-1").success()).isFalse();
    }

    @Test
    @DisplayName("Hasher: salted (same password, different hashes), verifies, rejects malformed and short passwords")
    void hasher() {
        String a = PasswordHasher.hash("password-123"), b = PasswordHasher.hash("password-123");
        assertThat(a).isNotEqualTo(b);
        assertThat(PasswordHasher.matches("password-123", a)).isTrue();
        assertThat(PasswordHasher.matches("password-124", a)).isFalse();
        assertThat(PasswordHasher.matches("password-123", "garbage")).isFalse();
        assertThat(PasswordHasher.matches("password-123", "pbkdf2$x$y$z")).isFalse();
        assertThatThrownBy(() -> PasswordHasher.hash("short")).isInstanceOf(ValidationException.class);
    }

    private UserProfileRepositoryPort svcUsers() {
        return new UserProfileRepositoryPort() {
            public Optional<UserProfile> findById(UserId id) { return Optional.empty(); }
            public Optional<UserProfile> findByUsername(String u) { return Optional.ofNullable(profiles.get(u)); }
            public List<UserProfile> findAll() { return List.of(); }
            public UserProfile save(UserProfile p) { return p; }
            public void deleteById(UserId id) {}
        };
    }

    private UserCredentialRepositoryPort svcStore() {
        return new UserCredentialRepositoryPort() {
            public Optional<Credential> find(String id) { return Optional.ofNullable(creds.get(id)); }
            public void savePassword(String id, String h) {}
            public void recordFailure(String id, int n, Instant until) { var c = creds.get(id); creds.put(id, new Credential(id, c.passwordHash(), n, until)); }
            public void clearFailures(String id) { var c = creds.get(id); creds.put(id, new Credential(id, c.passwordHash(), 0, null)); }
        };
    }
}
