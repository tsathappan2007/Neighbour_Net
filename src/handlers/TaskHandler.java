package handlers;

import models.Task;
import models.User;

/**
 * Functional interface for handling a Task.
 * Provides default life-cycle methods for task validation, execution, and dispute handling.
 */
@FunctionalInterface
public interface TaskHandler {
    /**
     * Handles the task processing.
     *
     * @param t the task to process
     */
    void handle(Task t);

    /**
     * Validates if a task can be processed based on category requirements.
     *
     * @param t the task to validate
     * @throws Exception if validation fails (e.g., insufficient trust score)
     */
    default void validate(Task t) throws Exception {
        // Default empty validation
    }

    /**
     * Executes the task specific operations.
     *
     * @param t the task to execute
     * @throws Exception if execution fails
     */
    default void execute(Task t) throws Exception {
        // Default empty execution
    }

    /**
     * Handles disputes raised on the task completion.
     *
     * @param t        the task under dispute
     * @param reason   the dispute reason
     * @param disputer the user raising the dispute
     * @throws Exception if dispute processing fails
     */
    default void handleDispute(Task t, String reason, User disputer) throws Exception {
        // Default dispute handling
    }
}
