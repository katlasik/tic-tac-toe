CREATE TABLE users_confirmation_emails (
  user_id UUID REFERENCES users(id),
  confirmation_token VARCHAR,
  created_on TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY(user_id, confirmation_token)
);
