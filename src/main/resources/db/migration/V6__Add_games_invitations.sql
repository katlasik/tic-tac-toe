CREATE TABLE game_invitations (
   id UUID PRIMARY KEY,
   owner_id UUID NOT NULL,
   guest_id UUID CHECK (guest_id IS NULL OR owner_id <> guest_id),
   guest_email EMAIL,
   token VARCHAR,
   accepted_on TIMESTAMP,
   rejected_on TIMESTAMP,
   cancelled_on TIMESTAMP,
   created_on TIMESTAMP DEFAULT NOW(),
   updated_on TIMESTAMP
);

CREATE TRIGGER set_updated_on_game_invitations_trigger BEFORE UPDATE ON game_invitations FOR EACH ROW EXECUTE PROCEDURE set_updated_on();
