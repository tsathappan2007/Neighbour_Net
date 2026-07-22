package services;

import java.util.LinkedList;
import java.util.Queue;
import models.Task;
import models.User;

/**
 * Service facilitating asynchronous processing of task completions and trust score updates.
 * Demonstrates Inter-Thread Communication using Object.wait() and Object.notifyAll() (Unit 4).
 */
public class InterThreadCommunication {
    private final Queue<CompletionEvent> queue = new LinkedList<>();
    private final TrustEngine<User> trustEngine;
    private final Thread consumerThread;
    private volatile boolean running = true;

    /**
     * Represents a task completion event to be processed.
     */
    public static class CompletionEvent {
        private final Task task;
        private final float rating; // Rating from 1.0 to 5.0
        private final String feedback;

        public CompletionEvent(Task task, float rating, String feedback) {
            this.task = task;
            this.rating = rating;
            this.feedback = feedback;
        }

        public Task getTask() { return task; }
        public float getRating() { return rating; }
        public String getFeedback() { return feedback; }
    }

    /**
     * Constructs the InterThreadCommunication manager and starts the background consumer.
     *
     * @param trustEngine the reputation scoring engine to update
     */
    public InterThreadCommunication(TrustEngine<User> trustEngine) {
        this.trustEngine = trustEngine;
        this.consumerThread = new Thread(this::consumeEvents, "TrustEngineConsumer");
        this.consumerThread.setDaemon(true);
        this.consumerThread.start();
    }

    /**
     * Submits a completed task event to the processing queue (Producer).
     * Synchronized on the queue object to ensure thread safety.
     *
     * @param task     the completed task
     * @param rating   the feedback rating given (1-5 stars)
     * @param feedback text feedback left by the task owner
     */
    public synchronized void submitCompletionEvent(Task task, float rating, String feedback) {
        queue.add(new CompletionEvent(task, rating, feedback));
        // Notify the consumer thread that a new event is available (Unit 4)
        notifyAll();
    }

    /**
     * Background consumer loop (Consumer).
     * Waits when queue is empty, wakes up on notification.
     */
    private void consumeEvents() {
        while (running) {
            CompletionEvent event = null;
            
            synchronized (this) {
                while (queue.isEmpty() && running) {
                    try {
                        // Wait until notified of new completion events (Unit 4)
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("[TrustConsumer] Thread interrupted.");
                        return;
                    }
                }
                if (!running) break;
                event = queue.poll();
            }

            if (event != null) {
                processEvent(event);
            }
        }
    }

    /**
     * Calculates the trust score delta based on ratings and applies it using TrustEngine.
     */
    private void processEvent(CompletionEvent event) {
        Task task = event.getTask();
        User helper = task.getAcceptedBy();
        if (helper == null) return;

        float rating = event.getRating();
        // Rating impact logic:
        // 5 stars: +2.0 trust
        // 4 stars: +1.0 trust
        // 3 stars: +0.0 trust
        // 2 stars: -2.0 trust
        // 1 star : -5.0 trust
        float delta = switch ((int) rating) {
            case 5 -> 2.0f;
            case 4 -> 1.0f;
            case 3 -> 0.0f;
            case 2 -> -2.0f;
            case 1 -> -5.0f;
            default -> 0.0f;
        };

        String reason = String.format("Completed Task %s ('%s') - Rated %.1f/5.0: %s",
                task.getTaskId(), task.getDescription(), rating, event.getFeedback());
        
        // Trust update using generic engine
        trustEngine.updateScore(helper, delta, reason, task.getPostedBy().getUserId());
    }

    /**
     * Gracefully stops the background consumer thread.
     */
    public synchronized void shutdown() {
        running = false;
        notifyAll();
    }
}
