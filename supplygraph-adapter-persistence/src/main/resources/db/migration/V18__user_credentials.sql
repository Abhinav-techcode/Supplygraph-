-- V18: login credentials (salted PBKDF2 hashes only; never the password) with failed-attempt lockout state
CREATE TABLE IF NOT EXISTS user_credentials (
    user_id VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(256) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
