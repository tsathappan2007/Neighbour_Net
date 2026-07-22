package exceptions;

import models.User;

/**
 * Thrown when an accepted task times out or the helper fails to show up or complete it.
 */
public class NoShowException extends Exception {
    private final String taskId;
    private final User accepter;

    /**
     * Constructs a NoShowException.
     *
     * @param taskId   the ID of the task
     * @param accepter the user who accepted the task but failed to complete it
     */
    public NoShowException(String taskId, User accepter) {
        super(String.format("User [%s] did not complete task [%s]", accepter.getName(), taskId));
        this.taskId = taskId;
        this.accepter = accepter;
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
     * Gets the user who failed to complete the task.
     *
     * @return the accepter/helper
     */
    public User getAccepter() {
        return accepter;
    }
}
