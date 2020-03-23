CREATE DOMAIN EMAIL VARCHAR CHECK (value ~ '[^@]+@[^@]+');
