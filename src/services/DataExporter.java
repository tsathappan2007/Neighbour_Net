package services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import models.Task;
import models.User;

/**
 * Service to export users and tasks from memory/database into CSV files.
 * Uses FileWriter and BufferedWriter to write files (Unit 3).
 */
public class DataExporter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Exports a list of users to 'users.csv'.
     *
     * @param users the collection of users to export
     * @throws IOException if file creation or writing fails
     */
    public static void exportUsers(Collection<User> users, String filepath) throws IOException {
        if (users == null) return;
        try (FileWriter fw = new FileWriter(filepath);
             BufferedWriter bw = new BufferedWriter(fw)) {
            
            // Header
            bw.write("userId,name,roomNo,role,trustScore");
            bw.newLine();
            
            for (User u : users) {
                String line = String.format("%s,%s,%s,%s,%.2f",
                        escapeCSV(u.getUserId()),
                        escapeCSV(u.getName()),
                        escapeCSV(u.getRoomNo()),
                        escapeCSV(u.getRole()),
                        u.getTrustScore());
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Exports a list of tasks to 'tasks.csv'.
     *
     * @param tasks the collection of tasks to export
     * @throws IOException if file creation or writing fails
     */
    public static void exportTasks(Collection<Task> tasks, String filepath) throws IOException {
        if (tasks == null) return;
        try (FileWriter fw = new FileWriter(filepath);
             BufferedWriter bw = new BufferedWriter(fw)) {
            
            // Header
            bw.write("taskId,category,description,status,postedBy,acceptedBy,createdAt,completedAt");
            bw.newLine();
            
            for (Task t : tasks) {
                String postedById = t.getPostedBy() != null ? t.getPostedBy().getUserId() : "";
                String acceptedById = t.getAcceptedBy() != null ? t.getAcceptedBy().getUserId() : "";
                String createdStr = t.getCreatedAt() != null ? t.getCreatedAt().format(formatter) : "";
                String completedStr = t.getCompletedAt() != null ? t.getCompletedAt().format(formatter) : "";

                String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCSV(t.getTaskId()),
                        escapeCSV(t.getCategory().name()),
                        escapeCSV(t.getDescription()),
                        escapeCSV(t.getStatus().name()),
                        escapeCSV(postedById),
                        escapeCSV(acceptedById),
                        escapeCSV(createdStr),
                        escapeCSV(completedStr));
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Helper to escape special characters for CSV formatting.
     */
    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
