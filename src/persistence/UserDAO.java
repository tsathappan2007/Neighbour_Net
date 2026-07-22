package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import models.User;

/**
 * Data Access Object (DAO) for the users table.
 * Demonstrates JDBC prepared statements and result set mapping (Unit 5).
 */
public class UserDAO {
    private final DatabaseConnection dbConn = DatabaseConnection.getInstance();

    /**
     * Saves a user to the database. Performs an INSERT OR REPLACE (upsert).
     *
     * @param u the user to save
     * @throws SQLException if a database error occurs
     */
    public void save(User u) throws SQLException {
        String sql = "INSERT OR REPLACE INTO users (user_id, name, room_no, role, trust_score, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getName());
            ps.setString(3, u.getRoomNo());
            ps.setString(4, u.getRole());
            ps.setFloat(5, u.getTrustScore());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Finds a user by ID.
     *
     * @param userId the user ID
     * @return the user object if found, otherwise null
     * @throws SQLException if a database error occurs
     */
    public User findById(String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of all registered users in the database.
     *
     * @return list of users
     * @throws SQLException if a database error occurs
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        }
        return list;
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the user ID to delete
     * @throws SQLException if a database error occurs
     */
    public void delete(String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates the user's trust score directly.
     *
     * @param userId   the user ID
     * @param newScore the new trust score
     * @throws SQLException if a database error occurs
     */
    public void updateTrustScore(String userId, float newScore) throws SQLException {
        String sql = "UPDATE users SET trust_score = ? WHERE user_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setFloat(1, newScore);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Helper to map a database ResultSet row to a User object.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("room_no"),
                rs.getString("role"),
                rs.getFloat("trust_score")
        );
    }
}
