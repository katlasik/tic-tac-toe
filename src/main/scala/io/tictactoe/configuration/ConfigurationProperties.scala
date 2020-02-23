package io.tictactoe.configuration

final case class Db(databaseUrl: String, driver: String)

final case class Server(host: String, port: Int)

final case class App(name: String, version: String)

final case class Docs(file: String)

final case class ConfigurationProperties(
    db: Db,
    server: Server,
    app: App,
    docs: Docs
)
