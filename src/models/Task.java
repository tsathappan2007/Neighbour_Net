package models;

import handlers.CompletableTask;
import java.time.LocalDateTime;

/**
 * Represents a micro-task in the NeighbourNet hyperlocal exchange.
 * Implements CompletableTask to support task completion flow.
 */
public class Task implements CompletableTask {
    private String taskId;
    private TaskCategory category;
    private String description;
    private TaskStatus status;
    private User postedBy;
    private User acceptedBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Proof cache
    private String proofNote;
    private User verifiedBy;
    private boolean verified;

    /**
     * Constructs a new Task with default status POSTED and current timestamp.
     *
     * @param taskId      the unique identifier of the task
     * @param category    the category of the task
     * @param description details describing the task
     * @param postedBy    the user who posted the task
     */
    public Task(String taskId, TaskCategory category, String description, User postedBy) {
        this.taskId = taskId;
        this.category = category;
        this.description = description;
        this.status = TaskStatus.POSTED;
        this.postedBy = postedBy;
        this.createdAt = LocalDateTime.now();
        this.verified = false;
    }

    /**
     * Full constructor for loading existing tasks from persistence.
     */
    public Task(String taskId, TaskCategory category, String description, TaskStatus status,
                User postedBy, User acceptedBy, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.taskId = taskId;
        this.category = category;
        this.description = description;
        this.status = status;
        this.postedBy = postedBy;
        this.acceptedBy = acceptedBy;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.verified = (status == TaskStatus.CLOSED);
    }

    /**
     * Gets the unique identifier of the task.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Sets the task ID.
     *
     * @param taskId the task ID to set
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Gets the category of the task.
     *
     * @return the task category
     */
    public TaskCategory getCategory() {
        return category;
    }

    /**
     * Sets the category of the task.
     *
     * @param category the category to set
     */
    public void setCategory(TaskCategory category) {
        this.category = category;
    }

    /**
     * Gets the description of the task.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the task.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the status of the task.
     *
     * @return the status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the task.
     *
     * @param status the status to set
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Gets the user who posted the task.
     *
     * @return the poster
     */
    public User getPostedBy() {
        return postedBy;
    }

    /**
     * Sets the user who posted the task.
     *
     * @param postedBy the poster to set
     */
    public void setPostedBy(User postedBy) {
        this.postedBy = postedBy;
    }

    /**
     * Gets the user who accepted/helped with the task.
     *
     * @return the helper user, or null if not accepted
     */
    public User getAcceptedBy() {
        return acceptedBy;
    }

    /**
     * Sets the user who accepted/helped with the task.
     *
     * @param acceptedBy the helper to set
     */
    public void setAcceptedBy(User acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    /**
     * Gets the timestamp when the task was posted.
     *
     * @return the creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when the task was posted.
     *
     * @param createdAt the creation time to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp when the task was completed.
     *
     * @return the completion time
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /**
     * Sets the timestamp when the task was completed.
     *
     * @param completedAt the completion time to set
     */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // CompletableTask implementations

    @Override
    public void markComplete(String proofText) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.proofNote = proofText;
    }

    @Override
    public void attachProof(String proofText, User verifier) {
        this.proofNote = proofText;
        this.verifiedBy = verifier;
        this.verified = true;
        this.status = TaskStatus.CLOSED;
    }

    @Override
    public boolean isVerified() {
        return verified;
    }

    /**
     * Gets the proof note text.
     *
     * @return the proof note, or null if none
     */
    public String getProofNote() {
        return proofNote;
    }

    /**
     * Gets the user who verified the proof.
     *
     * @return the verifier, or null if none
     */
    public User getVerifiedBy() {
        return verifiedBy;
    }

    @Override
    public String toString() {
        return String.format("Task[ID=%s, Category=%s, Status=%s, PostedBy=%s, AcceptedBy=%s, Desc='%s']",
                taskId, category, status, postedBy.getName(),
                (acceptedBy != null ? acceptedBy.getName() : "None"), description);
    }
}
