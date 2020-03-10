package io.tictactoe.configuration

import io.tictactoe.base.model.RedirectLocation
import io.tictactoe.values.Email

final case class Db(databaseUrl: String, driver: String)

final case class Server(host: String, port: Int)

final case class App(name: String, version: String, publicUrl: String)

final case class Docs(file: String)

final case class Smtp(host: String, port: Int)

final case class MailServer(username: String, password: String, smtp: Smtp)

final case class Mail(noReplyAddress: Email, server: MailServer)

final case class Registration(confirmationRedirect: RedirectLocation)

final case class ConfigurationProperties(
    db: Db,
    server: Server,
    app: App,
    docs: Docs,
    mail: Mail,
    registration: Registration
)
