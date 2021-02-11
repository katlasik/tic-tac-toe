# Tic-tac-toe

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/09ae4e37f6d84eafa1e887590b87263c)](https://app.codacy.com/manual/katlasik/tic-tac-toe?utm_source=github.com&utm_medium=referral&utm_content=katlasik/tic-tac-toe&utm_campaign=Badge_Grade_Dashboard)
[![CircleCI](https://circleci.com/gh/katlasik/tic-tac-toe.svg?style=svg)](https://circleci.com/gh/katlasik/tic-tac-toe)

An implementation of simple service allowing playing game of tic-tac-toe. It uses popular FP techniques as tagless-final. 

## Running locally

### Setting up infrastructure

To setup infrastructure locally you'd need to have `docker` and `docker-compose` installed. Go to [docker](docker) folder and type:

```bash
docker-compose up
``` 

### Setting up dns

Add following line to your local `etc/hosts`:

```bash
127.0.0.1       tictactoe.io
```

### Starting up application

Start application with:

```bash
sbt run
```

or if you want to enable live-reloading then run:

```bash
sbt ~reStart
```

### Tests

To run integration tests you'd need docker installed (for test-containers).

## Database 

To bootstrap database you can use script [Initialize_database.sql](src/main/resources/db/init/Initialize_database.sql).

On every startup of the application flyway runs [migrations](src/main/resources/db/migration).

## Documentation

Rest documentation is available on endpoint `/docs`.
