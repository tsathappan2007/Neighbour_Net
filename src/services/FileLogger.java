package services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe singleton logging class that writes logs to logs/neighbourhood.log.
 * Uses BufferedWriter and PrintWriter for file I/O (Unit 3).
 */
public class FileLogger {
    private static FileLogger instance;
    private static final String LOG_FILE_PATH = "logs/neighbourhood.log";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileLogger() {
        // Create logs directory if it doesn't exist
        File directory = new File("logs");
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Gets the singleton instance of the FileLogger.
     *
     * @return the logger instance
     */
    public static synchronized FileLogger getInstance() {
        if (instance == null) {
            instance = new FileLogger();
        }
        return instance;
    }

    /**
     * Writes a log entry in the format: [TIMESTAMP] USER | ACTION | DETAILS.
     * This method is synchronized to ensure thread safety when accessed concurrently.
     *
     * @param username the username or ID associated with the log
     * @param action   the action performed
     * @param details  extra details for the action
     */
    public synchronized void log(String username, String action, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logLine = String.format("[%s] %s | %s | %s", timestamp, username.toUpperCase(), action.toUpperCase(), details);
        
        // Print to console for review
        System.out.println(logLine);

        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(logLine);
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        }
    }
}
