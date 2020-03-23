CREATE TABLE users (
  id UUID PRIMARY KEY,
  name VARCHAR NOT NULL UNIQUE,
  hash VARCHAR NOT NULL,
  email EMAIL NOT NULL UNIQUE,
  is_confirmed BOOLEAN NOT NULL,
  created_on TIMESTAMP DEFAULT NOW(),
  confirmation_token VARCHAR,
  updated_on TIMESTAMP
);

CREATE TRIGGER set_updated_on_users_trigger BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE set_updated_on();
