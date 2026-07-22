package models;

import java.time.LocalDateTime;

/**
 * Represents a submission of task completion proof.
 */
public class CompletionProof {
    private String proofId;
    private String taskId;
    private String note;
    private User verifiedBy;
    private LocalDateTime createdAt;

    /**
     * Constructs a completion proof record.
     *
     * @param proofId    the unique identifier for the proof
     * @param taskId     the task being verified
     * @param note       notes or details describing the completed task
     * @param verifiedBy the user verifying the completion (requester of the task)
     * @param createdAt  timestamp when the proof was submitted
     */
    public CompletionProof(String proofId, String taskId, String note, User verifiedBy, LocalDateTime createdAt) {
        this.proofId = proofId;
        this.taskId = taskId;
        this.note = note;
        this.verifiedBy = verifiedBy;
        this.createdAt = createdAt;
    }

    /**
     * Gets the proof identifier.
     *
     * @return the proof ID
     */
    public String getProofId() {
        return proofId;
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
     * Gets the proof notes.
     *
     * @return the note
     */
    public String getNote() {
        return note;
    }

    /**
     * Gets the user who verified the completion.
     *
     * @return the verifier user
     */
    public User getVerifiedBy() {
        return verifiedBy;
    }

    /**
     * Gets the submission timestamp.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("CompletionProof[ID=%s, Task=%s, VerifiedBy=%s, Note='%s']",
                proofId, taskId, (verifiedBy != null ? verifiedBy.getName() : "None"), note);
    }
}
