package services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import models.Task;
import models.User;

/**
 * Registry of posted tasks maintaining their insertion order (FIFO) using a LinkedHashSet (Unit 5).
 * All methods are synchronized to provide thread safety.
 */
public class TaskRegistry {
    private final LinkedHashSet<Task> tasks = new LinkedHashSet<>();

    /**
     * Adds a task to the registry.
     *
     * @param task the task to add
     */
    public synchronized void add(Task task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    /**
     * Removes a task from the registry.
     *
     * @param task the task to remove
     */
    public synchronized void remove(Task task) {
        if (task != null) {
            tasks.remove(task);
        }
    }

    /**
     * Gets a copy of all tasks in the registry.
     *
     * @return list of tasks in FIFO order
     */
    public synchronized List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Filters tasks posted by or accepted by a user.
     * Demonstrates the Streams API (Unit 1).
     *
     * @param user the user to match
     * @return list of tasks related to the user
     */
    public synchronized List<Task> getTasksByUser(User user) {
        if (user == null) return new ArrayList<>();
        return tasks.stream()
                .filter(t -> t.getPostedBy().getUserId().equals(user.getUserId()) ||
                        (t.getAcceptedBy() != null && t.getAcceptedBy().getUserId().equals(user.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * Finds a task in the registry by its task ID.
     */
    public synchronized Task findById(String taskId) {
        if (taskId == null) return null;
        for (Task t : tasks) {
            if (t.getTaskId().equals(taskId)) {
                return t;
            }
        }
        return null;
    }
}
