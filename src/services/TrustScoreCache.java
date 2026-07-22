package services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance concurrent cache for user trust scores (Unit 5).
 * Uses ConcurrentHashMap for lock-free reads and thread-safe writes.
 */
public class TrustScoreCache {
    private final ConcurrentHashMap<String, Float> scoreMap = new ConcurrentHashMap<>();

    /**
     * Caches a user's trust score.
     *
     * @param userId the user ID
     * @param score  the trust score
     */
    public void put(String userId, float score) {
        if (userId != null) {
            scoreMap.put(userId, score);
        }
    }

    /**
     * Retrieves the cached trust score. Returns default 50.0 if not cached.
     *
     * @param userId the user ID
     * @return the cached score
     */
    public float get(String userId) {
        if (userId == null) return 50.0f;
        return scoreMap.getOrDefault(userId, 50.0f);
    }

    /**
     * Gets a copy of all cached trust scores.
     *
     * @return map of user ID to trust score
     */
    public Map<String, Float> getAllScores() {
        return new HashMap<>(scoreMap);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        scoreMap.clear();
    }
}
