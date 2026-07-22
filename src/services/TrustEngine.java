package services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import models.TrustUpdateRecord;
import models.User;
import persistence.TrustHistoryDAO;
import persistence.UserDAO;

/**
 * Generic trust and reputation scoring engine.
 * Demonstrates Generics (Unit 4) and Thread-Safety / Concurrency (Unit 4).
 *
 * @param <T> a type extending User
 */
public class TrustEngine<T extends User> {
    private final Map<String, T> userRegistry = Collections.synchronizedMap(new HashMap<>());
    private final List<TrustUpdateRecord<T>> history = Collections.synchronizedList(new ArrayList<>());
    
    private final UserDAO userDAO = new UserDAO();
    private final TrustHistoryDAO trustHistoryDAO = new TrustHistoryDAO();

    /**
     * Registers a user in the engine registry.
     *
     * @param user the user to register
     */
    public void registerUser(T user) {
        userRegistry.put(user.getUserId(), user);
    }

    /**
     * Atomically updates a user's trust score and writes to database history.
     * Demonstrates synchronized blocks for thread safety (Unit 4).
     *
     * @param user      the user whose score is updated
     * @param delta     the trust score change
     * @param reason    the reason for updating
     * @param updatedBy who triggered the update ("SYSTEM", or a user ID)
     */
    public void updateScore(T user, float delta, String reason, String updatedBy) {
        if (user == null) return;

        synchronized (user) {
            // Update in-memory user
            user.updateTrustScore(delta);
            float newScore = user.getTrustScore();

            // Create record
            String recordId = UUID.randomUUID().toString().substring(0, 8);
            TrustUpdateRecord<T> record = new TrustUpdateRecord<>(
                    recordId, user, null, delta, reason, LocalDateTime.now(), updatedBy
            );
            
            // Add to history
            history.add(record);

            // Persist to database
            try {
                userDAO.updateTrustScore(user.getUserId(), newScore);
                trustHistoryDAO.recordUpdate(record);
            } catch (Exception e) {
                System.err.println("[TrustEngine Error] Failed to persist trust update: " + e.getMessage());
            }

            // Log action
            FileLogger.getInstance().log(
                    updatedBy,
                    "TRUST_UPDATE",
                    String.format("User %s (%s) trust score changed by %.1f to %.1f. Reason: %s",
                            user.getName(), user.getUserId(), delta, newScore, reason)
            );
        }
    }

    /**
     * Gets the current trust score of a user.
     *
     * @param user the user
     * @return current trust score
     */
    public float getScore(T user) {
        if (user == null) return 0.0f;
        return user.getTrustScore();
    }

    /**
     * Retrieves all update history records for a user.
     * Uses Streams API to filter records (Unit 1).
     *
     * @param user the user
     * @return list of trust update records
     */
    public List<TrustUpdateRecord<T>> getHistory(T user) {
        if (user == null) return Collections.emptyList();
        
        synchronized (history) {
            return history.stream()
                    .filter(record -> record.getUser().getUserId().equals(user.getUserId()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Gets all registered users in the engine.
     *
     * @return list of users
     */
    public List<T> getAllUsers() {
        return new ArrayList<>(userRegistry.values());
    }

    /**
     * Finds a user in the registry by their ID.
     */
    public T findById(String userId) {
        return userRegistry.get(userId);
    }
}
