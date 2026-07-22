package models;

/**
 * Categories of tasks supported in NeighbourNet.
 */
public enum TaskCategory {
    ERRAND("Errands like picking up food, laundry, or groceries"),
    SKILL("Skilled tasks requiring specialized expertise like programming, repairing, or designing"),
    BORROW("Temporary loaning of items or tools"),
    TEACH("Mentorship, tutoring, or sharing academic knowledge");

    private final String description;

    TaskCategory(String description) {
        this.description = description;
    }

    /**
     * Gets a user-friendly description of the task category.
     *
     * @return the description of the category
     */
    public String getDescription() {
        return description;
    }
}
