CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  email VARCHAR(160) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  mfa_secret VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(32) NOT NULL UNIQUE
);
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);
CREATE TABLE resources (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL UNIQUE,
  type VARCHAR(20) NOT NULL,
  environment VARCHAR(8) NOT NULL,
  sensitivity VARCHAR(8) NOT NULL,
  path VARCHAR(200) NOT NULL
);
CREATE TABLE policies (
  id BIGSERIAL PRIMARY KEY,
  resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles(id),
  action VARCHAR(10) NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
  valid_from TIMESTAMPTZ,
  valid_until TIMESTAMPTZ,
  conditions VARCHAR(400)
);
CREATE INDEX idx_policies_res_role ON policies (resource_id, role_id);
CREATE TABLE devices (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  fingerprint VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  last_seen TIMESTAMPTZ,
  UNIQUE (user_id, fingerprint)
);
CREATE TABLE access_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  resource_id BIGINT NOT NULL REFERENCES resources(id),
  action VARCHAR(10) NOT NULL,
  reason VARCHAR(400),
  status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
  approved_by VARCHAR(64),
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE login_attempts (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  ip VARCHAR(64),
  success BOOLEAN NOT NULL,
  attempted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_login_user_time ON login_attempts (username, attempted_at);
CREATE TABLE audit_events (
  id BIGSERIAL PRIMARY KEY,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
  username VARCHAR(64) NOT NULL,
  resource_id BIGINT,
  resource_name VARCHAR(120),
  action VARCHAR(10),
  risk_score INT NOT NULL,
  device_risk INT NOT NULL DEFAULT 0,
  resource_risk INT NOT NULL DEFAULT 0,
  time_risk INT NOT NULL DEFAULT 0,
  auth_risk INT NOT NULL DEFAULT 0,
  decision VARCHAR(16) NOT NULL,
  device_status VARCHAR(20),
  ip VARCHAR(64),
  reason VARCHAR(500)
);
CREATE INDEX idx_audit_time ON audit_events (timestamp);
CREATE TABLE risk_config (
  key VARCHAR(64) PRIMARY KEY,
  value INT NOT NULL
);
