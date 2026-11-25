-- Per-user (time-bound) policies created when an access request is approved
ALTER TABLE policies ADD COLUMN user_id BIGINT REFERENCES users(id) ON DELETE CASCADE;
-- Single-use MFA tokens (keeps the backend stateless across replicas)
CREATE TABLE used_mfa_tokens (
  jti VARCHAR(64) PRIMARY KEY,
  used_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
