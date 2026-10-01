package reservo.database;

import reservo.common.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs database migrations from SQL files in resources/migrations/
 */
public class MigrationRunner {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    public static void runMigrations() {
        logger.info("Starting database migrations...");
        
        boolean useH2 = Config.getBoolean("db.use_h2", true);
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Set auto-commit to false for transaction safety
            conn.setAutoCommit(false);
            
            // Create migrations tracking table if it doesn't exist
            createMigrationsTable(conn);
            
            List<String> migrations = List.of(
                "001_initial_schema.sql",
                "002_seed_data.sql"
            );
            
            for (String migrationFile : migrations) {
                if (!isMigrationApplied(conn, migrationFile)) {
                    logger.info("Running migration: {}", migrationFile);
                    runMigration(conn, migrationFile);
                    recordMigration(conn, migrationFile);
                    conn.commit();
                    logger.info("Migration {} completed successfully", migrationFile);
                } else {
                    logger.debug("Migration {} already applied, skipping", migrationFile);
                }
            }
            
            logger.info("All migrations completed successfully");
            
        } catch (SQLException e) {
            logger.error("Migration failed", e);
            throw new RuntimeException("Database migration failed", e);
        }
    }

    private static void createMigrationsTable(Connection conn) throws SQLException {
        String createTable = useH2() ? 
            "CREATE TABLE IF NOT EXISTS schema_migrations (" +
            "    migration_name VARCHAR(255) PRIMARY KEY," +
            "    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")" :
            "CREATE TABLE IF NOT EXISTS schema_migrations (" +
            "    migration_name VARCHAR(255) PRIMARY KEY," +
            "    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
        }
    }

    private static boolean isMigrationApplied(Connection conn, String migrationName) throws SQLException {
        String sql = "SELECT 1 FROM schema_migrations WHERE migration_name = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, migrationName);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void recordMigration(Connection conn, String migrationName) throws SQLException {
        String sql = "INSERT INTO schema_migrations (migration_name) VALUES (?)";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, migrationName);
            ps.executeUpdate();
        }
    }

    private static void runMigration(Connection conn, String migrationFile) throws SQLException {
        String sql = loadMigrationFile(migrationFile);
        
        // Split by semicolon, but handle PostgreSQL functions and triggers carefully
        String[] statements = splitStatements(sql);
        
        try (Statement stmt = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        logger.error("Error executing statement in {}: {}", migrationFile, trimmed.substring(0, Math.min(100, trimmed.length())));
                        throw e;
                    }
                }
            }
        }
    }

    private static String loadMigrationFile(String filename) {
        try (InputStream is = MigrationRunner.class.getClassLoader()
                .getResourceAsStream("migrations/" + filename)) {
            if (is == null) {
                throw new RuntimeException("Migration file not found: " + filename);
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load migration file: " + filename, e);
        }
    }

    private static String[] splitStatements(String sql) {
        // Simple split by semicolon - in production, use a proper SQL parser
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;
        
        for (char c : sql.toCharArray()) {
            if ((c == '\'' || c == '"') && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                    stringChar = 0;
                }
            }
            
            current.append(c);
            
            if (!inString && c == ';') {
                statements.add(current.toString());
                current = new StringBuilder();
            }
        }
        
        if (current.length() > 0) {
            statements.add(current.toString());
        }
        
        return statements.toArray(new String[0]);
    }

    private static boolean useH2() {
        return Config.getBoolean("db.use_h2", true);
    }
}

