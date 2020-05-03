package io.tictactoe.utilities.database

final case class DatabaseConfig(
    username: String,
    password: String,
    jdbcUrl: String,
    name: String,
    port: Int,
    host: String
)
