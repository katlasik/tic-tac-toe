CREATE TABLE games (
   id UUID PRIMARY KEY REFERENCES game_invitations(id),
   owner_id UUID NOT NULL,
   guest_id UUID NOT NULL,
   initial_player_id UUID NOT NULL,
   created_on TIMESTAMP DEFAULT NOW()
);
