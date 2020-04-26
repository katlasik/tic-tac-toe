package io.tictactoe.testutils

import java.sql.DriverManager

import scala.io.Source
import scala.util.{Try, Using}

trait Database {_: Containers with ItTest =>

  protected def connection(port: Int) = DriverManager.getConnection(s"jdbc:postgresql://$host:$port/$dbName", dbUsername, dbPassword)

  def dbPort: Int = Try(sys.env("IT_TEST_DB_PORT").toInt).getOrElse(dbContainer.mappedPort(5432))

  def sql(scripts: String*): Unit = {
    val statement = connection(dbPort).createStatement()
    scripts.foreach(f => Using(Source.fromFile(s"src/it/resources/scripts/$f"))(l => statement.execute(l.mkString)))
  }

}
