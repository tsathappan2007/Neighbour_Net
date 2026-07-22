package models;

import java.time.LocalDateTime;

/**
 * Represents an entry in the trust history of a user.
 * This class is generic and accepts any type extending User.
 *
 * @param <T> a type that extends User
 */
public class TrustUpdateRecord<T extends User> {
    private String recordId;
    private T user;
    private String taskId;
    private float delta;
    private String reason;
    private LocalDateTime timestamp;
    private String updatedBy; // Can be "SYSTEM" or a userId

    /**
     * Constructs a trust score update record.
     *
     * @param recordId  the unique ID of this history log record
     * @param user      the user whose score was updated
     * @param taskId    the task ID associated with the update, or null
     * @param delta     the trust score change amount
     * @param reason    the description of why it was changed
     * @param timestamp when the change took place
     * @param updatedBy who triggered the update
     */
    public TrustUpdateRecord(String recordId, T user, String taskId, float delta,
                              String reason, LocalDateTime timestamp, String updatedBy) {
        this.recordId = recordId;
        this.user = user;
        this.taskId = taskId;
        this.delta = delta;
        this.reason = reason;
        this.timestamp = timestamp;
        this.updatedBy = updatedBy;
    }

    /**
     * Gets the record identifier.
     *
     * @return the record ID
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * Gets the user affected.
     *
     * @return the user
     */
    public T getUser() {
        return user;
    }

    /**
     * Gets the associated task ID.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the score delta.
     *
     * @return the delta value
     */
    public float getDelta() {
        return delta;
    }

    /**
     * Gets the update reason description.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the timestamp when the score changed.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets who updated the score.
     *
     * @return updater user ID or "SYSTEM"
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public String toString() {
        return String.format("[%s] User %s (%s) trust updated by %.1f. Reason: %s (By: %s)",
                timestamp, user.getName(), user.getUserId(), delta, reason, updatedBy);
    }
}
