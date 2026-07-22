package services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import models.CompletionProof;
import persistence.CompletionProofDAO;

/**
 * Stores and manages completion proofs chronologically using a LinkedHashMap (Unit 5).
 * Methods are synchronized to ensure thread safety across task completions.
 */
public class CompletionProofStore {
    private final LinkedHashMap<String, CompletionProof> proofMap = new LinkedHashMap<>();
    private final CompletionProofDAO proofDAO = new CompletionProofDAO();

    /**
     * Registers a completion proof.
     *
     * @param proof the completion proof to record
     */
    public synchronized void addProof(CompletionProof proof) {
        if (proof == null) return;
        proofMap.put(proof.getTaskId(), proof);
        try {
            proofDAO.save(proof);
        } catch (Exception e) {
            System.err.println("[CompletionProofStore Error] Failed to persist completion proof: " + e.getMessage());
        }
    }

    /**
     * Retrieves the completion proof for a specific task.
     *
     * @param taskId the task ID
     * @return the completion proof, or null if not found
     */
    public synchronized CompletionProof getProof(String taskId) {
        if (taskId == null) return null;
        return proofMap.get(taskId);
    }

    /**
     * Gets all proofs in chronological order.
     *
     * @return list of completion proofs
     */
    public synchronized List<CompletionProof> getAllProofs() {
        return new ArrayList<>(proofMap.values());
    }

    /**
     * Synchronizes and loads proofs from the database.
     */
    public synchronized void syncFromDatabase(List<String> taskIds) {
        try {
            proofMap.clear();
            for (String taskId : taskIds) {
                List<CompletionProof> proofs = proofDAO.findByTaskId(taskId);
                for (CompletionProof p : proofs) {
                    proofMap.put(p.getTaskId(), p);
                }
            }
            System.out.printf("[CompletionProofStore] Loaded %d proofs from database.\n", proofMap.size());
        } catch (Exception e) {
            System.err.println("[CompletionProofStore Sync Error] Failed to load proofs: " + e.getMessage());
        }
    }
}
