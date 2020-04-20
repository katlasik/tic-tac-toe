CREATE TABLE game_invitations (
   id UUID PRIMARY KEY,
   owner_id UUID NOT NULL,
   guest_id UUID CHECK (guest_id IS NULL OR owner_id <> guest_id),
   guest_email EMAIL,
   token VARCHAR,
   accepted_on TIMESTAMP CHECK(accepted_on IS NULL OR (accepted_on IS NOT NULL AND rejected_on IS NULL AND cancelled_on IS NULL)),
   rejected_on TIMESTAMP CHECK(rejected_on IS NULL OR (rejected_on IS NOT NULL AND accepted_on IS NULL AND cancelled_on IS NULL)),
   cancelled_on TIMESTAMP CHECK(cancelled_on IS NULL OR (cancelled_on IS NOT NULL AND rejected_on IS NULL AND rejected_on IS NULL)),
   created_on TIMESTAMP DEFAULT NOW(),
   updated_on TIMESTAMP
);

CREATE TRIGGER set_updated_on_game_invitations_trigger BEFORE UPDATE ON game_invitations FOR EACH ROW EXECUTE PROCEDURE set_updated_on();
