package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import models.Task;
import models.User;

/**
 * Service to notify potential helpers concurrently.
 * Demonstrates Concurrency by extending Thread and utilizing synchronization (Unit 4).
 */
public class NotificationService {
    private final List<String> notificationHistory = Collections.synchronizedList(new ArrayList<>());

    /**
     * Concurrently notifies a list of helpers about a task.
     * Spawns a thread for each notification to execute in parallel.
     *
     * @param task    the task available
     * @param helpers the list of users to notify
     */
    public void notifyHelpers(Task task, List<User> helpers) {
        if (task == null || helpers == null || helpers.isEmpty()) return;

        System.out.printf("[NotificationService] Dispatching concurrent notifications for task %s to %d helpers...\n",
                task.getTaskId(), helpers.size());

        for (User helper : helpers) {
            new NotificationThread(task, helper).start();
        }
    }

    /**
     * Thread implementation to simulate sending notification over the network.
     */
    private class NotificationThread extends Thread {
        private final Task task;
        private final User user;

        public NotificationThread(Task task, User user) {
            this.task = task;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                // Simulate network latency (Unit 4)
                Thread.sleep(500);

                String msg = String.format("[Notification for %s (%s)] A task near you: %s (Category: %s, Posted By: %s)",
                        user.getName(), user.getRoomNo(), task.getDescription(), task.getCategory(), task.getPostedBy().getName());
                
                // Print notification
                System.out.println(msg);

                // Add to synchronized history
                notificationHistory.add(msg);

                // Log notification
                FileLogger.getInstance().log("SYSTEM", "NOTIFIED", 
                        String.format("Notification sent to %s for task %s", user.getUserId(), task.getTaskId()));

            } catch (InterruptedException e) {
                System.err.println("Notification thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Gets all notifications sent in the current session.
     *
     * @return list of notification messages
     */
    public List<String> getNotificationHistory() {
        return new ArrayList<>(notificationHistory);
    }
}
