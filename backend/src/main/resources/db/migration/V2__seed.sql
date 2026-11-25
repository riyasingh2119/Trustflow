INSERT INTO roles (name) VALUES ('ADMIN'),('DEVELOPER'),('ANALYST'),('AUDITOR');

-- Demo password for all seeded users is "Passw0rd!" (BCrypt, cost 10)
INSERT INTO users (username, email, password_hash, enabled, mfa_secret) VALUES
 ('admin',   'admin@trustflow.local',   '$2a$10$Jz7bCkw53MGDVbBsK4mv1.s/0h6Zr7gkiNv4BNilR2czw7I5GgV.G', TRUE, 'ADMINSECRET0001'),
 ('dev',     'dev@trustflow.local',     '$2a$10$Jz7bCkw53MGDVbBsK4mv1.s/0h6Zr7gkiNv4BNilR2czw7I5GgV.G', TRUE, 'DEVSECRET000002'),
 ('analyst', 'analyst@trustflow.local', '$2a$10$Jz7bCkw53MGDVbBsK4mv1.s/0h6Zr7gkiNv4BNilR2czw7I5GgV.G', TRUE, 'ANLSECRET000003'),
 ('auditor', 'auditor@trustflow.local', '$2a$10$Jz7bCkw53MGDVbBsK4mv1.s/0h6Zr7gkiNv4BNilR2czw7I5GgV.G', TRUE, 'AUDSECRET000004');

INSERT INTO user_roles SELECT u.id, r.id FROM users u, roles r WHERE
 (u.username='admin' AND r.name='ADMIN') OR (u.username='dev' AND r.name='DEVELOPER')
 OR (u.username='analyst' AND r.name='ANALYST') OR (u.username='auditor' AND r.name='AUDITOR');

INSERT INTO resources (name, type, environment, sensitivity, path) VALUES
 ('Dev Orders API',        'API',        'DEV',  'LOW',    '/res/dev-orders-api'),
 ('Prod Customer DB',      'DATABASE',   'PROD', 'HIGH',   '/res/prod-customer-db'),
 ('Payments Repository',   'REPOSITORY', 'PROD', 'MEDIUM', '/res/payments-repo'),
 ('Prod Config Store',     'CONFIG',     'PROD', 'HIGH',   '/res/prod-config'),
 ('Analytics Service',     'SERVICE',    'PROD', 'MEDIUM', '/res/analytics-service');

INSERT INTO policies (resource_id, role_id, action, status)
SELECT res.id, r.id, a.action, 'ACTIVE' FROM resources res, roles r, (VALUES ('READ'),('WRITE')) AS a(action)
WHERE (res.name='Dev Orders API' AND r.name='DEVELOPER')
   OR (res.name='Prod Customer DB' AND r.name='DEVELOPER' AND a.action='READ')
   OR (res.name='Payments Repository' AND r.name='DEVELOPER')
   OR (res.name='Analytics Service' AND r.name='ANALYST' AND a.action='READ')
   OR (res.name='Prod Customer DB' AND r.name='ANALYST' AND a.action='READ');

INSERT INTO policies (resource_id, role_id, action, status)
SELECT res.id, r.id, a.action, 'ACTIVE' FROM resources res, roles r, (VALUES ('READ'),('WRITE'),('DELETE'),('ADMIN')) AS a(action)
WHERE r.name='ADMIN';

INSERT INTO policies (resource_id, role_id, action, status)
SELECT res.id, r.id, 'READ', 'ACTIVE' FROM resources res, roles r WHERE r.name='AUDITOR';

INSERT INTO devices (user_id, fingerprint, status, last_seen)
SELECT u.id, 'laptop-managed-01', 'MANAGED_COMPLIANT', now() FROM users u WHERE u.username IN ('admin','dev','analyst','auditor');
INSERT INTO devices (user_id, fingerprint, status, last_seen)
SELECT u.id, 'phone-byod-02', 'UNKNOWN', now() FROM users u WHERE u.username='dev';
INSERT INTO devices (user_id, fingerprint, status, last_seen)
SELECT u.id, 'old-pc-03', 'NON_COMPLIANT', now() FROM users u WHERE u.username='dev';

INSERT INTO risk_config (key, value) VALUES
 ('device.unknown', 15), ('device.noncompliant', 30),
 ('resource.low', 5), ('resource.medium', 15), ('resource.high', 30),
 ('time.shoulder', 7), ('time.night', 15),
 ('auth.per_failure', 5), ('auth.cap', 25), ('auth.window_minutes', 15),
 ('threshold.allow_max', 30), ('threshold.mfa_max', 60);
