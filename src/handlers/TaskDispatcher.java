package handlers;

import models.Task;
import models.TaskStatus;

/**
 * Dispatches tasks to their category-specific handlers and performs status transition validation.
 * Demonstrates Java 21 pattern matching for switch statements and guard clauses.
 */
public class TaskDispatcher {
    private final TaskHandler errandHandler = new ErrandHandler();
    private final TaskHandler skillHandler = new SkillHandler();
    private final TaskHandler borrowHandler = new BorrowHandler();
    private final TaskHandler teachHandler = new TeachHandler();

    /**
     * Dispatches the task to the correct handler based on its category.
     *
     * @param task the task to dispatch
     */
    public void dispatch(Task task) {
        if (task == null) return;
        
        // Java 21 switch pattern matching for task categories
        switch (task.getCategory()) {
            case ERRAND -> errandHandler.handle(task);
            case SKILL -> skillHandler.handle(task);
            case BORROW -> borrowHandler.handle(task);
            case TEACH -> teachHandler.handle(task);
        }
    }

    /**
     * Handles dispute routing for a task based on its category.
     */
    public void dispatchDispute(Task task, String reason, models.User disputer) throws Exception {
        TaskHandler handler = switch (task.getCategory()) {
            case ERRAND -> errandHandler;
            case SKILL -> skillHandler;
            case BORROW -> borrowHandler;
            case TEACH -> teachHandler;
        };
        handler.handleDispute(task, reason, disputer);
    }

    /**
     * Inspects a task and prints details using Java 21 switch pattern matching with guard clauses.
     *
     * @param task the task to inspect
     */
    public void inspectTask(Task task) {
        if (task == null) {
            System.out.println("Task is null.");
            return;
        }

        // Switch pattern matching with guard clauses (Java 21 'when') on Task object type pattern
        switch (task) {
            case Task t when t.getStatus() == TaskStatus.POSTED -> 
                System.out.printf("[Dispatcher Log] Task %s is POSTED and waiting for a helper.\n", t.getTaskId());
            case Task t when t.getStatus() == TaskStatus.ACCEPTED && t.getAcceptedBy() != null -> 
                System.out.printf("[Dispatcher Log] Task %s is ACCEPTED by helper %s.\n", t.getTaskId(), t.getAcceptedBy().getName());
            case Task t when t.getStatus() == TaskStatus.COMPLETED && t.getProofNote() != null -> 
                System.out.printf("[Dispatcher Log] Task %s is COMPLETED. Proof: '%s'.\n", t.getTaskId(), t.getProofNote());
            case Task t when t.getStatus() == TaskStatus.DISPUTED -> 
                System.out.printf("[Dispatcher Log] Task %s is DISPUTED. Immediate review needed!\n", t.getTaskId());
            case Task t when t.getStatus() == TaskStatus.CLOSED -> 
                System.out.printf("[Dispatcher Log] Task %s is CLOSED and finalized.\n", t.getTaskId());
            default -> 
                System.out.printf("[Dispatcher Log] Task %s is in an inconsistent state.\n", task.getTaskId());
        }
    }

    /**
     * Validates if the transition to a new status is valid.
     *
     * @param current the current status
     * @param next    the target status
     * @return true if transition is allowed, false otherwise
     */
    public boolean isValidTransition(TaskStatus current, TaskStatus next) {
        return switch (current) {
            case POSTED -> next == TaskStatus.ACCEPTED || next == TaskStatus.CLOSED;
            case ACCEPTED -> next == TaskStatus.COMPLETED || next == TaskStatus.DISPUTED || next == TaskStatus.CLOSED;
            case COMPLETED -> next == TaskStatus.CLOSED || next == TaskStatus.DISPUTED;
            case DISPUTED -> next == TaskStatus.CLOSED || next == TaskStatus.COMPLETED;
            case CLOSED -> false; // Terminal status
        };
    }
}
