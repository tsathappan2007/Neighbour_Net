package models;

/**
 * Represents a User in the NeighbourNet system.
 * A user can post and accept tasks, and maintains a trust score reflecting their reputation.
 */
public class User {
    private String userId;
    private String name;
    private String roomNo;
    private String role;
    private float trustScore;

    /**
     * Constructs a new User with default trust score of 50.0.
     *
     * @param userId   the unique identifier of the user
     * @param name     the name of the user
     * @param roomNo   the room number of the user
     * @param role     the role of the user (e.g. resident, student, staff)
     */
    public User(String userId, String name, String roomNo, String role) {
        this.userId = userId;
        this.name = name;
        this.roomNo = roomNo;
        this.role = role;
        this.trustScore = 50.0f;
    }

    /**
     * Constructs a User with a specific trust score (used when loading from the database).
     *
     * @param userId     the unique identifier of the user
     * @param name       the name of the user
     * @param roomNo     the room number of the user
     * @param role       the role of the user
     * @param trustScore the current trust score of the user
     */
    public User(String userId, String name, String roomNo, String role, float trustScore) {
        this.userId = userId;
        this.name = name;
        this.roomNo = roomNo;
        this.role = role;
        this.trustScore = trustScore;
    }

    /**
     * Gets the unique identifier of the user.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier of the user.
     *
     * @param userId the user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's room number.
     *
     * @return the room number
     */
    public String getRoomNo() {
        return roomNo;
    }

    /**
     * Sets the user's room number.
     *
     * @param roomNo the room number to set
     */
    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    /**
     * Gets the user's role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the user's role.
     *
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
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
     * Sets the user's trust score directly.
     *
     * @param trustScore the trust score to set
     */
    public void setTrustScore(float trustScore) {
        this.trustScore = Math.max(0.0f, Math.min(100.0f, trustScore));
    }

    /**
     * Updates the user's trust score by a delta, bounded between 0.0 and 100.0.
     *
     * @param delta the change in trust score
     */
    public synchronized void updateTrustScore(float delta) {
        this.trustScore = Math.max(0.0f, Math.min(100.0f, this.trustScore + delta));
    }

    @Override
    public String toString() {
        return String.format("User[ID=%s, Name=%s, Room=%s, Role=%s, Trust=%.1f]",
                userId, name, roomNo, role, trustScore);
    }
}
