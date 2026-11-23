package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.application.port.out.UserCredentialRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/** Credential store: salted hashes and lockout state in {@code user_credentials}. */
@Component
public class PostgresUserCredentialAdapter implements UserCredentialRepositoryPort {
    private final JdbcTemplate jdbc;

    public PostgresUserCredentialAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Credential> find(String userId) {
        return jdbc.query("SELECT * FROM user_credentials WHERE user_id = ?", (rs, i) -> new Credential(rs.getString("user_id"), rs.getString("password_hash"),
                rs.getInt("failed_attempts"), rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant()), userId).stream().findFirst();
    }

    @Override
    public void savePassword(String userId, String passwordHash) {
        Timestamp now = Timestamp.from(Instant.now());
        int n = jdbc.update("UPDATE user_credentials SET password_hash = ?, failed_attempts = 0, locked_until = NULL, updated_at = ? WHERE user_id = ?", passwordHash, now, userId);
        if (n == 0) jdbc.update("INSERT INTO user_credentials (user_id, password_hash, failed_attempts, locked_until, updated_at) VALUES (?,?,0,NULL,?)", userId, passwordHash, now);
    }

    @Override
    public void recordFailure(String userId, int failedAttempts, Instant lockedUntil) {
        jdbc.update("UPDATE user_credentials SET failed_attempts = ?, locked_until = ?, updated_at = ? WHERE user_id = ?", failedAttempts,
                lockedUntil == null ? null : Timestamp.from(lockedUntil), Timestamp.from(Instant.now()), userId);
    }

    @Override
    public void clearFailures(String userId) {
        jdbc.update("UPDATE user_credentials SET failed_attempts = 0, locked_until = NULL, updated_at = ? WHERE user_id = ?", Timestamp.from(Instant.now()), userId);
    }
}
