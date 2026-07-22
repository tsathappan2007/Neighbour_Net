package exceptions;

import models.User;

/**
 * Thrown when a task completion is disputed by the requester.
 */
public class DisputeException extends Exception {
    private final String taskId;
    private final String reason;
    private final User disputer;

    /**
     * Constructs a DisputeException.
     *
     * @param taskId   the ID of the disputed task
     * @param disputer the user raising the dispute
     * @param reason   the reason for the dispute
     */
    public DisputeException(String taskId, User disputer, String reason) {
        super(String.format("Task [%s] disputed by [%s]: %s", taskId, disputer.getName(), reason));
        this.taskId = taskId;
        this.disputer = disputer;
        this.reason = reason;
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
     * Gets the dispute reason.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the user who raised the dispute.
     *
     * @return the disputer user
     */
    public User getDisputer() {
        return disputer;
    }
}
