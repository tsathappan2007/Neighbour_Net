package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import models.TrustUpdateRecord;
import models.User;

/**
 * Data Access Object (DAO) for trust_history table.
 * Manages history records for reputation score modifications (Unit 5).
 */
public class TrustHistoryDAO {
    private final DatabaseConnection dbConn = DatabaseConnection.getInstance();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Records a new trust update in the database.
     *
     * @param record the update record to insert
     * @throws SQLException if a database error occurs
     */
    public void recordUpdate(TrustUpdateRecord<? extends User> record) throws SQLException {
        String sql = "INSERT INTO trust_history (record_id, user_id, task_id, delta, reason, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getRecordId());
            ps.setString(2, record.getUser().getUserId());
            ps.setString(3, record.getTaskId()); // Could be null
            ps.setFloat(4, record.getDelta());
            ps.setString(5, record.getReason());
            ps.setTimestamp(6, record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Finds all trust update history for a given user.
     *
     * @param userId the user to query
     * @return list of trust update records
     * @throws SQLException if a database error occurs
     */
    public List<TrustUpdateRecord<User>> findByUserId(String userId) throws SQLException {
        String sql = "SELECT * FROM trust_history WHERE user_id = ? ORDER BY updated_at DESC";
        List<TrustUpdateRecord<User>> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Maps database row to TrustUpdateRecord.
     */
    private TrustUpdateRecord<User> mapRowToRecord(ResultSet rs) throws SQLException {
        String userId = rs.getString("user_id");
        User user = userDAO.findById(userId);
        Timestamp ts = rs.getTimestamp("updated_at");

        return new TrustUpdateRecord<>(
                rs.getString("record_id"),
                user,
                rs.getString("task_id"),
                rs.getFloat("delta"),
                rs.getString("reason"),
                ts != null ? ts.toLocalDateTime() : null,
                "SYSTEM"
        );
    }
}
