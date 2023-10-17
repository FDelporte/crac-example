package be.webtechie.crac.example.manager;

import be.webtechie.crac.example.model.AppLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;

public class DatabaseManager implements Resource {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseManager.class);
    private static Connection connection = null;

    public DatabaseManager() {
        Core.getGlobalContext().register(this);
        initConnection();
    }

    public void initConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.warn("Setting up database connection");
                String url = "jdbc:postgresql://crac.local:5432/crac";
                String user = "cracApp";
                String password = "crac123";
                connection = DriverManager.getConnection(url, user, password);
                if (!connection.isClosed()) {
                    LOGGER.info("Database connection status: {}", connection.getClientInfo());
                } else {
                    LOGGER.error("Database connection is not available");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL error: {}", e.getMessage());
        }
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        LOGGER.info("Executing beforeCheckpoint");
        if (connection != null) {
            try {
                save(new AppLog("Closing DB connection before checkpoint"));
                save(new AppLog("==================================================="));
                Thread.sleep(500);
                connection.close();
                connection = null;
            } catch (InterruptedException | SQLException e) {
                LOGGER.error("Error while closing the connection: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        LOGGER.info("Executing afterRestore");
        initConnection();
        save(new AppLog("==================================================="));
        save(new AppLog("Reopened DB connection after restore"));
    }

    public Collection<AppLog> getAll() {
        Collection<AppLog> all = new ArrayList<>();
        String sql = "SELECT * FROM app_log ORDER BY timestamp DESC";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                var appLog = new AppLog(resultSet.getInt("id"),
                        resultSet.getTimestamp("timestamp").toInstant().atZone(ZoneId.of("UTC")),
                        resultSet.getInt("duration"),
                        resultSet.getString("description"));
                all.add(appLog);
                LOGGER.debug("Found {} in database", appLog);
            }

        } catch (SQLException e) {
            LOGGER.error("Error while reading from database: {}", e.getMessage());
        }
        return all;
    }

    public void save(AppLog appLog) {
        if (appLog == null) {
            LOGGER.error("AppLog to be saved can not be null");
            return;
        }

        String sql = "INSERT INTO "
                + "app_log(timestamp, duration, description) "
                + "VALUES(?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(appLog.getTimestamp().toLocalDateTime()));
            statement.setInt(2, appLog.getDuration());
            statement.setString(3, appLog.getDescription());
            int numberOfInsertedRows = statement.executeUpdate();
            LOGGER.debug("AppLog saved: {}", (numberOfInsertedRows > 0));
        } catch (SQLException e) {
            LOGGER.error("Error while saving AppLog to database: {}", e.getMessage());
        }
    }
}
