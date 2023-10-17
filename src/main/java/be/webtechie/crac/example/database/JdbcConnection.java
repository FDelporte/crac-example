package be.webtechie.crac.example.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcConnection implements Resource {

    private static final Logger LOGGER = LogManager.getLogger(JdbcConnection.class);
    private static Connection connection = null;

    public JdbcConnection() {
        Core.getGlobalContext().register(this);
    }

    public static Connection getConnection() {
        if (connection == null) {
            String url = "jdbc:postgresql://crac.local:5432/crac";
            String user = "cracApp";
            String password = "crac123";

            try {
                connection = DriverManager.getConnection(url, user, password);
                if (!connection.isClosed()) {
                    LOGGER.info("Database connection status: {}", connection.getClientInfo());
                } else {
                    LOGGER.error("Database connection is not available");
                }
            } catch (SQLException e) {
                LOGGER.error("SQL error: {}", e.getMessage());
            }
        }

        return connection;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        LOGGER.info("Executing beforeCheckpoint");
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                LOGGER.error("SQL error while closing the connection: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        LOGGER.info("Executing afterRestore");
        getConnection();
    }
}
