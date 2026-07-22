package handlers;

import exceptions.InsufficientTrustException;
import models.Task;
import models.TaskStatus;
import models.User;

/**
 * Handler for ERRAND category tasks.
 * Requires a minimum helper trust score of 45.0.
 */
public class ErrandHandler implements TaskHandler {
    private static final float MIN_TRUST_SCORE = 45.0f;

    @Override
    public void handle(Task t) {
        try {
            validate(t);
            execute(t);
        } catch (Exception e) {
            System.err.println("[ErrandHandler Error] " + e.getMessage());
        }
    }

    @Override
    public void validate(Task t) throws Exception {
        User helper = t.getAcceptedBy();
        if (helper != null) {
            if (t.getPostedBy().getUserId().equals(helper.getUserId())) {
                throw new IllegalArgumentException("A user cannot accept their own errand task.");
            }
            if (helper.getTrustScore() < MIN_TRUST_SCORE) {
                throw new InsufficientTrustException(helper.getUserId(), helper.getTrustScore(), MIN_TRUST_SCORE);
            }
        }
    }

    @Override
    public void execute(Task t) throws Exception {
        System.out.printf("[ErrandHandler] Executing errand task ID: %s. Description: %s. Helper: %s\n",
                t.getTaskId(), t.getDescription(),
                (t.getAcceptedBy() != null ? t.getAcceptedBy().getName() : "Unassigned"));
        // Transition status if appropriate
        if (t.getAcceptedBy() != null && t.getStatus() == TaskStatus.POSTED) {
            t.setStatus(TaskStatus.ACCEPTED);
        }
    }

    @Override
    public void handleDispute(Task t, String reason, User disputer) throws Exception {
        System.out.printf("[ErrandHandler] Handling dispute on task %s. Disputed by %s. Reason: %s\n",
                t.getTaskId(), disputer.getName(), reason);
        t.setStatus(TaskStatus.DISPUTED);
    }
}
