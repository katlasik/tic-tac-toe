CREATE TABLE emails (
  id UUID PRIMARY KEY,
  recipients EMAIL[] CHECK (array_length(recipients, 1) > 0),
  sender EMAIL,
  text VARCHAR,
  title VARCHAR,
  created_on TIMESTAMP DEFAULT NOW(),
  sent_on TIMESTAMP NULL
);
