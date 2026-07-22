package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import models.Task;
import models.TaskCategory;
import models.TaskStatus;
import models.User;

/**
 * Data Access Object (DAO) for the tasks table.
 * Demonstrates prepared statements and result set mapping with joins/relational loads (Unit 5).
 */
public class TaskDAO {
    private final DatabaseConnection dbConn = DatabaseConnection.getInstance();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Saves a task to the database (INSERT OR REPLACE).
     *
     * @param t the task to save
     * @throws SQLException if a database error occurs
     */
    public void save(Task t) throws SQLException {
        String sql = "INSERT OR REPLACE INTO tasks (task_id, category, description, status, posted_by, accepted_by, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTaskId());
            ps.setString(2, t.getCategory().name());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getStatus().name());
            ps.setString(5, t.getPostedBy().getUserId());
            ps.setString(6, t.getAcceptedBy() != null ? t.getAcceptedBy().getUserId() : null);
            ps.setTimestamp(7, t.getCreatedAt() != null ? Timestamp.valueOf(t.getCreatedAt()) : null);
            ps.setTimestamp(8, t.getCompletedAt() != null ? Timestamp.valueOf(t.getCompletedAt()) : null);
            ps.executeUpdate();
        }
    }

    /**
     * Finds a task by ID.
     */
    public Task findById(String taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns all tasks.
     */
    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks";
        List<Task> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToTask(rs));
            }
        }
        return list;
    }

    /**
     * Finds tasks by status.
     */
    public List<Task> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE status = ?";
        List<Task> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds tasks by category.
     */
    public List<Task> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE category = ?";
        List<Task> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates only the status and completedAt fields of a task.
     */
    public void updateStatus(String taskId, String newStatus, Timestamp completedAt) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, completed_at = ? WHERE task_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, completedAt);
            ps.setString(3, taskId);
            ps.executeUpdate();
        }
    }

    /**
     * Maps database ResultSet row to a Task model object.
     */
    private Task mapRowToTask(ResultSet rs) throws SQLException {
        String postedByUserId = rs.getString("posted_by");
        String acceptedByUserId = rs.getString("accepted_by");

        User postedBy = userDAO.findById(postedByUserId);
        User acceptedBy = acceptedByUserId != null ? userDAO.findById(acceptedByUserId) : null;

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp completedTs = rs.getTimestamp("completed_at");

        return new Task(
                rs.getString("task_id"),
                TaskCategory.valueOf(rs.getString("category")),
                rs.getString("description"),
                TaskStatus.valueOf(rs.getString("status")),
                postedBy,
                acceptedBy,
                createdTs != null ? createdTs.toLocalDateTime() : null,
                completedTs != null ? completedTs.toLocalDateTime() : null
        );
    }
}
