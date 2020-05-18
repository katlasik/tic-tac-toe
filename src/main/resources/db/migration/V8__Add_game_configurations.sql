CREATE TABLE game_configurations (
   id UUID PRIMARY KEY REFERENCES game_invitations(id),
   wining_length SMALLINT,
   size SMALLINT
);
