package reservo.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import reservo.common.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connections using HikariCP connection pool.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        HikariConfig config = new HikariConfig();

        boolean useH2 = Config.getBoolean("db.use_h2", true);
        if (useH2) {
            config.setJdbcUrl(Config.get("db.url", "jdbc:h2:mem:reservo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"));
            config.setDriverClassName("org.h2.Driver");
            config.setUsername(Config.get("db.user", "sa"));
            config.setPassword(Config.get("db.password", ""));
        } else {
            config.setJdbcUrl(Config.get("db.url", "jdbc:postgresql://localhost:5432/reservo"));
            config.setDriverClassName("org.postgresql.Driver");
            config.setUsername(Config.get("db.user", "postgres"));
            config.setPassword(Config.get("db.password", "postgres"));
        }

        config.setMaximumPoolSize(Config.getInt("db.pool.max_size", 20));
        config.setMinimumIdle(Config.getInt("db.pool.size", 10));
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        dataSource = new HikariDataSource(config);
        initialized = true;
        logger.info("Database connection pool initialized (H2: {})", useH2);
    }

    public static DataSource getDataSource() {
        if (!initialized) {
            initialize();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            initialized = false;
            logger.info("Database connection pool closed");
        }
    }

    public static void setIsolationLevel(String level) throws SQLException {
        int isolationLevel;
        switch (level.toUpperCase()) {
            case "READ_UNCOMMITTED":
                isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
                break;
            case "READ_COMMITTED":
                isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
                break;
            case "REPEATABLE_READ":
                isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
                break;
            case "SERIALIZABLE":
                isolationLevel = Connection.TRANSACTION_SERIALIZABLE;
                break;
            default:
                isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
        }

        try (Connection conn = getConnection()) {
            conn.setTransactionIsolation(isolationLevel);
            logger.info("Isolation level set to: {}", level);
        }
    }

    public static String getCurrentIsolationLevel() throws SQLException {
        try (Connection conn = getConnection()) {
            int level = conn.getTransactionIsolation();
            switch (level) {
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    return "READ_UNCOMMITTED";
                case Connection.TRANSACTION_READ_COMMITTED:
                    return "READ_COMMITTED";
                case Connection.TRANSACTION_REPEATABLE_READ:
                    return "REPEATABLE_READ";
                case Connection.TRANSACTION_SERIALIZABLE:
                    return "SERIALIZABLE";
                default:
                    return "UNKNOWN";
            }
        }
    }
}

