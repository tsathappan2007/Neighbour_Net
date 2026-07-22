package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import models.User;
import persistence.UserDAO;

/**
 * In-memory repository of registered users utilizing an ArrayList (Unit 5).
 * Synchronized methods ensure thread-safe access to user profiles.
 */
public class UserRepository {
    private final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private final UserDAO userDAO = new UserDAO();

    /**
     * Registers a new user in the repository and database.
     *
     * @param user the user to register
     */
    public void addUser(User user) {
        if (user == null) return;
        users.add(user);
        try {
            userDAO.save(user);
            FileLogger.getInstance().log("SYSTEM", "REGISTER_USER", 
                    String.format("User registered: %s (ID: %s)", user.getName(), user.getUserId()));
        } catch (Exception e) {
            System.err.println("[UserRepository Error] Failed to save user: " + e.getMessage());
        }
    }

    /**
     * Removes a user from the repository and database.
     *
     * @param userId the ID of the user to delete
     */
    public void deleteUser(String userId) {
        if (userId == null) return;
        users.removeIf(u -> u.getUserId().equals(userId));
        try {
            userDAO.delete(userId);
            FileLogger.getInstance().log("SYSTEM", "DELETE_USER", "Deleted user ID: " + userId);
        } catch (Exception e) {
            System.err.println("[UserRepository Error] Failed to delete user: " + e.getMessage());
        }
    }

    /**
     * Finds a user by their user ID.
     *
     * @param userId the user ID to search
     * @return the user object, or null if not found
     */
    public User findById(String userId) {
        if (userId == null) return null;
        synchronized (users) {
            for (User u : users) {
                if (u.getUserId().equals(userId)) {
                    return u;
                }
            }
        }
        return null;
    }

    /**
     * Gets a copy of the list of registered users.
     *
     * @return list of users
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    /**
     * Loads all users from the database into the memory cache.
     */
    public void syncFromDatabase() {
        try {
            List<User> dbUsers = userDAO.findAll();
            users.clear();
            users.addAll(dbUsers);
            System.out.printf("[UserRepository] Successfully loaded %d users from database.\n", users.size());
        } catch (Exception e) {
            System.err.println("[UserRepository Sync Error] Failed to load users: " + e.getMessage());
        }
    }

    /**
     * Persists all currently cached users back to the database.
     */
    public void syncToDatabase() {
        try {
            synchronized (users) {
                for (User u : users) {
                    userDAO.save(u);
                }
            }
            System.out.println("[UserRepository] Successfully flushed memory users to database.");
        } catch (Exception e) {
            System.err.println("[UserRepository Sync Error] Failed to save users: " + e.getMessage());
        }
    }
}
