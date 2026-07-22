package services;

import java.util.List;
import java.util.stream.Collectors;
import models.Task;
import models.TaskCategory;
import models.User;

/**
 * Service providing high-level operations on tasks using Java Streams (Unit 1).
 */
public class TaskService {
    private final TaskRegistry taskRegistry;
    private final UserRepository userRepository;

    /**
     * Constructs a TaskService.
     *
     * @param taskRegistry   the task registry to load tasks from
     * @param userRepository the user repository to find helpers from
     */
    public TaskService(TaskRegistry taskRegistry, UserRepository userRepository) {
        this.taskRegistry = taskRegistry;
        this.userRepository = userRepository;
    }

    /**
     * Filters tasks in the registry by their category.
     * Demonstrates the Streams API and Collectors (Unit 1).
     *
     * @param category the category to filter by
     * @return list of tasks matching the category
     */
    public List<Task> getTasksByCategory(TaskCategory category) {
        return taskRegistry.getTasks().stream()
                .filter(t -> t.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Filters tasks where the poster's trust score meets a certain threshold.
     * Demonstrates the Streams API and filtering (Unit 1).
     *
     * @param threshold the minimum trust score threshold
     * @return list of tasks
     */
    public List<Task> getTasksByTrustThreshold(float threshold) {
        return taskRegistry.getTasks().stream()
                .filter(t -> t.getPostedBy().getTrustScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * Finds nearby helpers for a given task using the Streams API.
     * Evaluates room proximity (same block letter) and trust score thresholds.
     *
     * @param t             the task needing help
     * @param minTrustScore the minimum trust score required for helpers
     * @return list of potential helpers nearby
     */
    public List<User> findNearbyHelpers(Task t, float minTrustScore) {
        if (t == null) return List.of();
        
        String posterRoom = t.getPostedBy().getRoomNo();
        String posterId = t.getPostedBy().getUserId();

        return userRepository.getAllUsers().stream()
                // Helper cannot be the poster
                .filter(u -> !u.getUserId().equals(posterId))
                // Helper must have sufficient reputation
                .filter(u -> u.getTrustScore() >= minTrustScore)
                // Hyperlocal matching: same block letter (e.g. 'A' in 'A102' matches 'A550')
                .filter(u -> !posterRoom.isEmpty() && !u.getRoomNo().isEmpty() &&
                        u.getRoomNo().charAt(0) == posterRoom.charAt(0))
                .collect(Collectors.toList());
    }
}
