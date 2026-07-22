package handlers;

import models.User;

/**
 * Interface representing a task that can be completed, verified, and have completion proof attached.
 */
public interface CompletableTask {
    /**
     * Marks the task as completed.
     *
     * @param proofText the explanation or confirmation of completion
     */
    void markComplete(String proofText);

    /**
     * Attaches verification proof to the task.
     *
     * @param proofText the verification notes
     * @param verifier  the user validating the completion
     */
    void attachProof(String proofText, User verifier);

    /**
     * Checks if the task's completion is verified.
     *
     * @return true if verified, false otherwise
     */
    boolean isVerified();
}
