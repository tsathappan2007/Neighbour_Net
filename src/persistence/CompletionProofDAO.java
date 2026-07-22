package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import models.CompletionProof;
import models.User;

/**
 * Data Access Object (DAO) for the completion_proofs table.
 * Maps task completion verifications to SQLite (Unit 5).
 */
public class CompletionProofDAO {
    private final DatabaseConnection dbConn = DatabaseConnection.getInstance();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Saves a completion proof to the database.
     *
     * @param proof the proof to save
     * @throws SQLException if a database error occurs
     */
    public void save(CompletionProof proof) throws SQLException {
        String sql = "INSERT OR REPLACE INTO completion_proofs (proof_id, task_id, note, verified_by, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proof.getProofId());
            ps.setString(2, proof.getTaskId());
            ps.setString(3, proof.getNote());
            ps.setString(4, proof.getVerifiedBy() != null ? proof.getVerifiedBy().getUserId() : null);
            ps.setTimestamp(5, proof.getCreatedAt() != null ? Timestamp.valueOf(proof.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Finds a completion proof by ID.
     */
    public CompletionProof findById(String proofId) throws SQLException {
        String sql = "SELECT * FROM completion_proofs WHERE proof_id = ?";
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proofId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProof(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds all completion proofs associated with a task ID.
     */
    public List<CompletionProof> findByTaskId(String taskId) throws SQLException {
        String sql = "SELECT * FROM completion_proofs WHERE task_id = ?";
        List<CompletionProof> list = new ArrayList<>();
        try (Connection conn = dbConn.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToProof(rs));
                }
            }
        }
        return list;
    }

    /**
     * Maps database row to CompletionProof object.
     */
    private CompletionProof mapRowToProof(ResultSet rs) throws SQLException {
        String verifiedByUserId = rs.getString("verified_by");
        User verifiedBy = verifiedByUserId != null ? userDAO.findById(verifiedByUserId) : null;
        Timestamp ts = rs.getTimestamp("created_at");

        return new CompletionProof(
                rs.getString("proof_id"),
                rs.getString("task_id"),
                rs.getString("note"),
                verifiedBy,
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
