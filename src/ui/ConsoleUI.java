package ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import models.Task;
import models.User;

/**
 * Text-based user interface using BufferedReader to read console input (Unit 3).
 * Formats menus and outputs to present a premium experience in the terminal.
 */
public class ConsoleUI {
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Renders the main title banner and options menu.
     */
    public void displayMenu() {
        System.out.println("\n" + "=".repeat(46));
        System.out.println("          🏠  NEIGHBOURNET TASK EXCHANGE  🏠          ");
        System.out.println("=".repeat(46));
        System.out.println("  1. 👤 Register User");
        System.out.println("  2. 📝 Post a Hyperlocal Task");
        System.out.println("  3. 🔍 View Available Tasks");
        System.out.println("  4. 🤝 Accept Task");
        System.out.println("  5. ✅ Complete Task & Attach Proof");
        System.out.println("  6. ⭐ Rate Helper (Update Trust Score)");
        System.out.println("  7. 📊 View User Profile & History");
        System.out.println("  8. 📥 Export System Data (CSV)");
        System.out.println("  9. 🚪 Exit");
        System.out.println("=".repeat(46));
        System.out.print("Select an option (1-9): ");
    }

    /**
     * Reads a line of user input.
     *
     * @return the trimmed input string, or empty string on error
     */
    public String getUserInput() {
        try {
            String line = reader.readLine();
            return line != null ? line.trim() : "";
        } catch (Exception e) {
            System.err.println("Error reading user input: " + e.getMessage());
            return "";
        }
    }

    /**
     * Prompts the user with a message and reads a string response.
     */
    public String readString(String prompt) {
        System.out.print(prompt);
        return getUserInput();
    }

    /**
     * Prompts the user and reads an integer within a range.
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = getUserInput();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("Please enter an integer between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Try again.");
            }
        }
    }

    /**
     * Prompts the user and reads a float within a range.
     */
    public float readFloat(String prompt, float min, float max) {
        while (true) {
            System.out.print(prompt);
            String input = getUserInput();
            try {
                float val = Float.parseFloat(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("Please enter a decimal number between %.1f and %.1f.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid decimal number format. Try again.");
            }
        }
    }

    /**
     * Formats and prints a single user profile.
     */
    public void displayUserProfile(User user, List<models.TrustUpdateRecord<User>> history) {
        if (user == null) {
            System.out.println("User profile is null.");
            return;
        }
        System.out.println("\n┌──────────────────────────────────────────────┐");
        System.out.printf("│  USER PROFILE: %-29s │\n", user.getName());
        System.out.println("├──────────────────────────────────────────────┤");
        System.out.printf("│  User ID:    %-31s │\n", user.getUserId());
        System.out.printf("│  Room No:    %-31s │\n", user.getRoomNo());
        System.out.printf("│  Role:       %-31s │\n", user.getRole());
        System.out.printf("│  Reputation: %-31s │\n", String.format("%.1f/100.0", user.getTrustScore()));
        System.out.println("└──────────────────────────────────────────────┘");

        if (history != null && !history.isEmpty()) {
            System.out.println("\n📜 Trust Score History (Recent first):");
            for (var record : history) {
                System.out.printf(" - [%s] Delta: %+.1f | Reason: %s (By: %s)\n",
                        record.getTimestamp().toString().substring(0, 19).replace('T', ' '),
                        record.getDelta(),
                        record.getReason(),
                        record.getUpdatedBy());
            }
        } else {
            System.out.println("No score updates recorded for this user yet.");
        }
    }

    /**
     * Displays a list of tasks in a clean, formatted table.
     */
    public void displayTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("No tasks to display.");
            return;
        }
        
        System.out.println("\n" + "-".repeat(110));
        System.out.printf("%-8s | %-8s | %-12s | %-12s | %-12s | %-50s\n",
                "TASK ID", "CATEGORY", "STATUS", "POSTER", "HELPER", "DESCRIPTION");
        System.out.println("-".repeat(110));
        
        for (Task t : tasks) {
            String poster = t.getPostedBy() != null ? t.getPostedBy().getName() : "Unknown";
            String helper = t.getAcceptedBy() != null ? t.getAcceptedBy().getName() : "Unassigned";
            
            // Limit description to 50 chars for clean alignment
            String desc = t.getDescription();
            if (desc.length() > 50) {
                desc = desc.substring(0, 47) + "...";
            }

            System.out.printf("%-8s | %-8s | %-12s | %-12s | %-12s | %-50s\n",
                    t.getTaskId(), t.getCategory().name(), t.getStatus().name(),
                    poster, helper, desc);
        }
        System.out.println("-".repeat(110));
    }
}
