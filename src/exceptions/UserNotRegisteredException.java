package exceptions;

/**
 * Thrown when a user ID is referenced that does not exist in the system registry.
 */
public class UserNotRegisteredException extends Exception {
    private final String userId;

    /**
     * Constructs a UserNotRegisteredException.
     *
     * @param userId the ID of the unregistered user
     */
    public UserNotRegisteredException(String userId) {
        super(String.format("User with ID [%s] is not registered in the system", userId));
        this.userId = userId;
    }

    /**
     * Gets the unregistered user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }
}
