package persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database connection manager for SQLite.
 * Handles schema initialization and connection pooling simulation (Unit 5).
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private static final String DB_URL = "jdbc:sqlite:data/neighbournet.db";
    private Connection connection;

    private DatabaseConnection() {
        // Create data directory if it doesn't exist
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            // Force load SQLite JDBC Driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    /**
     * Gets the singleton instance of the connection manager.
     *
     * @return the connection manager instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Returns a Connection to the SQLite database.
     * If connection is closed or null, re-establishes it.
     *
     * @return active Database Connection
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    /**
     * Closes the active database connection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Initializes the database schema using statements from 'schema.sql'.
     * Demonstrates I/O File Streams (Unit 3) and JDBC Statement execution (Unit 5).
     */
    public void initializeSchema() {
        File schemaFile = new File("schema.sql");
        if (!schemaFile.exists()) {
            System.err.println("schema.sql not found! Cannot initialize database schema.");
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {

            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip empty lines and SQL comments
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                sqlBuilder.append(line).append("\n");
                
                // Execute individual statements terminated by semicolon
                if (trimmed.endsWith(";")) {
                    stmt.execute(sqlBuilder.toString());
                    sqlBuilder.setLength(0); // Reset for the next statement
                }
            }
            System.out.println("[DatabaseConnection] Schema initialized successfully from schema.sql.");

        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }
}
