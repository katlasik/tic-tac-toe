package io.tictactoe.utilities.configuration.model

import io.tictactoe.values.Email

final case class Db(databaseUrl: String, driver: String)

final case class Server(host: String, port: Int)

final case class App(name: String, version: String, publicUrl: String)

final case class Smtp(host: String, port: Int)

final case class MailServer(username: String, password: String, smtp: Smtp)

final case class Mail(noReplyAddress: Email, server: MailServer)

final case class ConfigurationProperties(
    db: Db,
    server: Server,
    app: App,
    mail: Mail
)
