CREATE OR REPLACE FUNCTION set_updated_on()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = now();
    RETURN NEW;
END;
$$ language 'plpgsql';
