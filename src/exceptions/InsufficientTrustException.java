package exceptions;

/**
 * Thrown when a user attempts to accept or execute a task but their trust score is below the minimum required.
 */
public class InsufficientTrustException extends Exception {
    private final String userId;
    private final float trustScore;
    private final float requiredScore;

    /**
     * Constructs an InsufficientTrustException.
     *
     * @param userId        the user ID
     * @param trustScore    the user's current trust score
     * @param requiredScore the minimum required trust score for the task
     */
    public InsufficientTrustException(String userId, float trustScore, float requiredScore) {
        super(String.format("User [%s] has insufficient trust score (current: %.1f, required: %.1f)",
                userId, trustScore, requiredScore));
        this.userId = userId;
        this.trustScore = trustScore;
        this.requiredScore = requiredScore;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the user's trust score.
     *
     * @return the trust score
     */
    public float getTrustScore() {
        return trustScore;
    }

    /**
     * Gets the required trust score.
     *
     * @return the required score
     */
    public float getRequiredScore() {
        return requiredScore;
    }
}
