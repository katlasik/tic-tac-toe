CREATE DATABASE tictactoe;
CREATE USER tictactoe_user WITH ENCRYPTED PASSWORD 'tictactoe';
GRANT ALL PRIVILEGES ON DATABASE tictactoe TO tictactoe_user;
