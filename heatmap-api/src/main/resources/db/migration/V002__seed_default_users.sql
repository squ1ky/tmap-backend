INSERT INTO users (id, email, password_hash, nickname, role, blocked)
VALUES
  (
    '00000000-0000-0000-0000-000000000001',
    'admin@tmap.local',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6i8fD5ZCwPWXLmvHNKMaePZtW6V1a',
    'admin',
    'ADMIN',
    false
  ),
  (
    '11111111-1111-1111-1111-111111111111',
    'user@tmap.local',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6i8fD5ZCwPWXLmvHNKMaePZtW6V1a',
    'user',
    'USER',
    false
  );
