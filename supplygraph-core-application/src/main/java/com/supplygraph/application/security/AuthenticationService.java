package com.supplygraph.application.security;

import com.supplygraph.application.port.out.UserCredentialRepositoryPort;
import com.supplygraph.application.port.out.UserProfileRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Username and password authentication. Every failure returns the same message (no hint whether the user exists), unknown users still pay
 * the hashing cost, and an account is locked for a few minutes after repeated failures.
 */
@Service
public class AuthenticationService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK = Duration.ofMinutes(5);
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password");

    public record Result(boolean success, UserProfile user, String message) {
        static Result ok(UserProfile u) { return new Result(true, u, null); }
        static Result fail(String m) { return new Result(false, null, m); }
    }

    private final UserProfileRepositoryPort users;
    private final UserCredentialRepositoryPort credentials;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthenticationService(UserProfileRepositoryPort users, UserCredentialRepositoryPort credentials) {
        this(users, credentials, Clock.systemUTC());
    }

    AuthenticationService(UserProfileRepositoryPort users, UserCredentialRepositoryPort credentials, Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.clock = clock;
    }

    public Result authenticate(String username, String password) {
        String generic = "Wrong username or password.";
        if (username == null || username.isBlank() || password == null || password.isEmpty()) return Result.fail(generic);
        Optional<UserProfile> user = users.findByUsername(username.trim().toLowerCase());
        Optional<UserCredentialRepositoryPort.Credential> cred = user.flatMap(u -> credentials.find(u.id().value()));
        if (user.isEmpty() || cred.isEmpty() || !user.get().active()) {
            PasswordHasher.matches(password, DUMMY_HASH);   // keep timing similar for unknown users
            return Result.fail(generic);
        }
        Instant now = clock.instant();
        UserCredentialRepositoryPort.Credential c = cred.get();
        if (c.lockedUntil() != null && c.lockedUntil().isAfter(now)) {
            long minutes = Math.max(1, Duration.between(now, c.lockedUntil()).toMinutes() + 1);
            return Result.fail("Too many failed attempts. Try again in about " + minutes + " minute(s).");
        }
        if (PasswordHasher.matches(password, c.passwordHash())) {
            if (c.failedAttempts() > 0 || c.lockedUntil() != null) credentials.clearFailures(c.userId());
            return Result.ok(user.get());
        }
        int failures = c.failedAttempts() + 1;
        Instant lock = failures >= MAX_FAILURES ? now.plus(LOCK) : null;
        credentials.recordFailure(c.userId(), lock != null ? 0 : failures, lock);
        if (lock != null) log.warn("Account {} locked until {} after {} failed sign-ins", username, lock, failures);
        return Result.fail(lock != null ? "Too many failed attempts. The account is locked for " + LOCK.toMinutes() + " minutes." : generic);
    }

    /** Creates the user (or updates the password of an existing one). Used for first-run bootstrap and administration. */
    public UserProfile register(String username, String displayName, String email, UserRole role, String password, WorkspaceId workspace) {
        String name = username.trim().toLowerCase();
        UserProfile profile = users.findByUsername(name).orElseGet(() -> users.save(new UserProfile(UserId.of("usr-" + name), name, email, displayName, role, Set.of(workspace), true, clock.instant())));
        credentials.savePassword(profile.id().value(), PasswordHasher.hash(password));
        return profile;
    }

    public boolean hasAnyCredential() { return users.findAll().stream().anyMatch(u -> credentials.find(u.id().value()).isPresent()); }
}
