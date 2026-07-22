package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import models.Task;
import models.TaskCategory;
import models.User;

/**
 * Service that runs matching algorithms concurrently using an ExecutorService.
 * Demonstrates Concurrency (Unit 4) with Futures and Callables.
 */
public class TaskMatchingEngine {
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final List<User> allUsers;

    /**
     * Constructs a TaskMatchingEngine.
     *
     * @param allUsers a list of all active users in the system to match against
     */
    public TaskMatchingEngine(List<User> allUsers) {
        this.allUsers = allUsers;
    }

    /**
     * Asynchronously matches a task with potential helpers near the poster.
     *
     * @param task the task to find helpers for
     * @return a Future containing the list of matching nearby helper users
     */
    public Future<List<User>> matchTaskAsync(Task task) {
        return executorService.submit(new Callable<List<User>>() {
            @Override
            public List<User> call() throws Exception {
                List<User> matchedHelpers = Collections.synchronizedList(new ArrayList<>());
                
                // Get task category threshold
                float minTrust = getRequiredTrust(task.getCategory());
                String posterRoom = task.getPostedBy().getRoomNo();
                String posterId = task.getPostedBy().getUserId();

                // Match users in parallel/streams
                allUsers.parallelStream().forEach(u -> {
                    // Conditions: not the poster, has sufficient trust score, and is "nearby"
                    if (!u.getUserId().equals(posterId) && u.getTrustScore() >= minTrust) {
                        if (isNearby(posterRoom, u.getRoomNo())) {
                            matchedHelpers.add(u);
                        }
                    }
                });

                // Simulate processing time
                Thread.sleep(300);

                return matchedHelpers;
            }
        });
    }

    /**
     * Determines if two room numbers are considered nearby (hyperlocal).
     * e.g., Same hostel wing/block (same prefix letter) and same floor (same floor number).
     */
    private boolean isNearby(String room1, String room2) {
        if (room1 == null || room2 == null || room1.isEmpty() || room2.isEmpty()) {
            return false;
        }
        // Extract block (first character, e.g., 'A' in 'A101')
        char block1 = room1.charAt(0);
        char block2 = room2.charAt(0);
        
        if (block1 != block2) return false;

        // If they have floors (digits following block), check floor proximity
        if (room1.length() >= 2 && room2.length() >= 2) {
            char floor1 = room1.charAt(1);
            char floor2 = room2.charAt(1);
            return floor1 == floor2; // true if same floor
        }
        return true;
    }

    /**
     * Helper to get category required trust thresholds.
     */
    private float getRequiredTrust(TaskCategory category) {
        return switch (category) {
            case ERRAND -> 45.0f;
            case SKILL -> 60.0f;
            case BORROW -> 50.0f;
            case TEACH -> 55.0f;
        };
    }

    /**
     * Shuts down the ExecutorService.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
