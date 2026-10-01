package reservo.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader for the application.
 * Supports loading from properties file or environment variables.
 */
public class Config {
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    static {
        load();
    }

    private static void load() {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties, using defaults");
        }

        // Override with environment variables if present
        overrideWithEnv();

        loaded = true;
    }

    private static void overrideWithEnv() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null) props.setProperty("db.url", dbUrl);

        String dbUser = System.getenv("DB_USER");
        if (dbUser != null) props.setProperty("db.user", dbUser);

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null) props.setProperty("db.password", dbPassword);

        String serverPort = System.getenv("SERVER_PORT");
        if (serverPort != null) props.setProperty("server.port", serverPort);

        String useH2 = System.getenv("USE_H2");
        if (useH2 != null) props.setProperty("db.use_h2", useH2);
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    public static Properties getAll() {
        return new Properties(props);
    }
}

