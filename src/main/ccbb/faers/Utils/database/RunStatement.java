package main.ccbb.faers.Utils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunStatement {
  final static Logger logger = LogManager.getLogger(RunStatement.class);

  public static void executeAStatement(Connection conn, String sqlString) throws SQLException {
    Statement stmt = conn.createStatement();
    stmt.execute(sqlString);
    stmt.close();

    logger.info("execute:" + sqlString);

  }

}
