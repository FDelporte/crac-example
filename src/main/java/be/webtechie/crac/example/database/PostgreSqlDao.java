package be.webtechie.crac.example.database;

import be.webtechie.crac.example.manager.DatabaseConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class PostgreSqlDao implements Dao<AppLog, Integer> {

    private static final Logger LOGGER = LogManager.getLogger(PostgreSqlDao.class);
    private final Connection connection;

    public PostgreSqlDao(DatabaseConnectionManager connection) {
        this.connection = DatabaseConnectionManager.getConnection();
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

    public Optional<AppLog> get(int id) {
        String sql = "SELECT * FROM app_log WHERE id = " + id;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                var appLog = new AppLog(resultSet.getInt("id"),
                        resultSet.getTimestamp("timestamp").toInstant().atZone(ZoneId.of("UTC")),
                        resultSet.getInt("duration"),
                        resultSet.getString("description"));
                LOGGER.debug("Found {} in database", appLog.getId());
                return Optional.of(appLog);
            }
        } catch (SQLException e) {
            LOGGER.error("Error while reading from database: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> save(AppLog appLog) {
        if (appLog == null) {
            LOGGER.error("AppLog to be saved can not be null");
            return Optional.empty();
        }

        String sql = "INSERT INTO "
                + "app_log(timestamp, duration, description) "
                + "VALUES(?, ?, ?)";

        Optional<Integer> generatedId = Optional.empty();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(appLog.getTimestamp().toLocalDateTime()));
            statement.setInt(2, appLog.getDuration());
            statement.setString(3, appLog.getDescription());

            int numberOfInsertedRows = statement.executeUpdate();

            // Retrieve the auto-generated id
            if (numberOfInsertedRows > 0) {
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        generatedId = Optional.of(resultSet.getInt(1));
                    }
                }
            }

            LOGGER.debug("AppLog saved? {}", (numberOfInsertedRows > 0));
        } catch (SQLException e) {
            LOGGER.error("Error while saving AppLog to database: {}", e.getMessage());
        }

        return generatedId;
    }
}
