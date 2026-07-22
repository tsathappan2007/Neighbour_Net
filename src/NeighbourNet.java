import exceptions.InsufficientTrustException;
import handlers.TaskDispatcher;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import models.CompletionProof;
import models.Task;
import models.TaskCategory;
import models.TaskStatus;
import models.User;
import persistence.DatabaseConnection;
import persistence.TaskDAO;
import persistence.UserDAO;
import services.DataExporter;
import services.FileLogger;
import services.InterThreadCommunication;
import services.NotificationService;
import services.TaskMatchingEngine;
import services.TaskRegistry;
import services.TaskService;
import services.TrustEngine;
import services.UserRepository;
import ui.ConsoleUI;

/**
 * Main Entry Point for NeighbourNet Application.
 * Hyperlocal task and help exchange console application demonstrating Course Units 1-5.
 */
public class NeighbourNet {
    private static final ConsoleUI ui = new ConsoleUI();
    private static final TaskDAO taskDAO = new TaskDAO();
    private static final UserDAO userDAO = new UserDAO();

    // Registries and Repositories (Unit 5 Collections)
    private static final UserRepository userRepository = new UserRepository();
    private static final TaskRegistry taskRegistry = new TaskRegistry();
    private static final services.CompletionProofStore proofStore = new services.CompletionProofStore();

    // Engines and Services (Unit 4 Concurrency & Generics)
    private static TrustEngine<User> trustEngine;
    private static InterThreadCommunication interThreadComm;
    private static TaskMatchingEngine matchingEngine;
    private static NotificationService notificationService;
    private static TaskService taskService;
    private static TaskDispatcher taskDispatcher;

    /**
     * Application main method. Sets up database, launches services, runs UI loop.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.out.println("Initializing NeighbourNet Core services...");
        
        // 1. Initialize Database Schema (Unit 5 JDBC)
        DatabaseConnection.getInstance().initializeSchema();

        try {
            // 2. Generate Demo Data on First Run (Unit 5 JDBC operations)
            generateDemoData();
        } catch (Exception e) {
            System.err.println("Error generating demo dataset: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Load Persistent Data into Memory Collections
        System.out.println("Syncing memory caches with database...");
        userRepository.syncFromDatabase();
        
        try {
            List<Task> dbTasks = taskDAO.findAll();
            for (Task t : dbTasks) {
                taskRegistry.add(t);
            }
            System.out.printf("[TaskRegistry] Loaded %d tasks from database.\n", dbTasks.size());
        } catch (Exception e) {
            System.err.println("Failed to load tasks from database: " + e.getMessage());
        }

        // Sync Proofs
        List<String> taskIds = new ArrayList<>();
        for (Task t : taskRegistry.getTasks()) {
            taskIds.add(t.getTaskId());
        }
        proofStore.syncFromDatabase(taskIds);

        // 4. Initialize Core Engine & Concurrency Layers
        trustEngine = new TrustEngine<>();
        for (User u : userRepository.getAllUsers()) {
            trustEngine.registerUser(u);
        }

        // Consumer thread for rating events (Unit 4 wait/notify)
        interThreadComm = new InterThreadCommunication(trustEngine);
        
        // Parallel matching pool executor (Unit 4 ExecutorService)
        matchingEngine = new TaskMatchingEngine(userRepository.getAllUsers());
        
        // Spawns notifier threads (Unit 4 Thread)
        notificationService = new NotificationService();

        // Streams processor (Unit 1 Streams)
        taskService = new TaskService(taskRegistry, userRepository);

        // Task handling and routing dispatcher (Unit 2 Pattern Matching)
        taskDispatcher = new TaskDispatcher();

        System.out.println("NeighbourNet Hyperlocal Exchange is online!");
        FileLogger.getInstance().log("SYSTEM", "STARTUP", "NeighbourNet application started successfully.");

        // 5. User Input CLI Loop
        boolean running = true;
        while (running) {
            ui.displayMenu();
            String choice = ui.getUserInput();
            switch (choice) {
                case "1" -> handleRegisterUser();
                case "2" -> handlePostTask();
                case "3" -> handleViewTasks();
                case "4" -> handleAcceptTask();
                case "5" -> handleCompleteTask();
                case "6" -> handleRateUser();
                case "7" -> handleViewProfile();
                case "8" -> handleExportData();
                case "9" -> {
                    running = false;
                    System.out.println("\nShutting down active services...");
                }
                default -> System.out.println("Invalid choice. Please select a option between 1 and 9.");
            }
        }

        // 6. Graceful Shutdown & Connections Release
        interThreadComm.shutdown();
        matchingEngine.shutdown();
        DatabaseConnection.getInstance().closeConnection();
        FileLogger.getInstance().log("SYSTEM", "SHUTDOWN", "NeighbourNet application closed.");
        System.out.println("Thank you for using NeighbourNet! Safe building.");
    }

    /**
     * Registers a new User through CLI prompt (Option 1).
     */
    private static void handleRegisterUser() {
        System.out.println("\n--- Register User ---");
        String name = ui.readString("Enter Name: ");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        String room = ui.readString("Enter Room No (e.g. A102): ");
        if (room.isEmpty()) {
            System.out.println("Room number cannot be empty.");
            return;
        }
        String role = ui.readString("Enter Role (e.g. Student, Resident, Tutor): ");
        
        // Auto-generate UserId
        String userId = "U" + (100 + userRepository.getAllUsers().size() + 1);
        User newUser = new User(userId, name, room, role);

        // Add to cache & persist
        userRepository.addUser(newUser);
        trustEngine.registerUser(newUser);

        System.out.printf("User registered successfully! Assigned User ID: %s\n", userId);
    }

    /**
     * Posts a new micro-task (Option 2).
     * Automatically triggers asynchronous helper matching and notification.
     */
    private static void handlePostTask() {
        System.out.println("\n--- Post a Task ---");
        String posterId = ui.readString("Enter your User ID: ");
        User poster = userRepository.findById(posterId);
        if (poster == null) {
            System.out.println("Error: User ID not found in database.");
            return;
        }

        System.out.println("\nSelect Task Category:");
        System.out.println(" 1. ERRAND - Pickup laundry/groceries (Min Trust Helper Required: 45.0)");
        System.out.println(" 2. SKILL  - Development, Repairs   (Min Trust Helper Required: 60.0)");
        System.out.println(" 3. BORROW - Tools, Chargers, Bikes  (Min Trust Helper Required: 50.0)");
        System.out.println(" 4. TEACH  - Tutoring, Mentorship    (Min Trust Helper Required: 55.0)");
        int catChoice = ui.readInt("Choice (1-4): ", 1, 4);

        TaskCategory category = switch (catChoice) {
            case 1 -> TaskCategory.ERRAND;
            case 2 -> TaskCategory.SKILL;
            case 3 -> TaskCategory.BORROW;
            default -> TaskCategory.TEACH;
        };

        String description = ui.readString("Enter short task description: ");
        if (description.isEmpty()) {
            System.out.println("Description cannot be empty.");
            return;
        }

        // Generate task ID
        String taskId = "T" + (1000 + taskRegistry.getTasks().size() + 1);
        Task task = new Task(taskId, category, description, poster);

        // Add to Registry
        taskRegistry.add(task);
        try {
            taskDAO.save(task);
            FileLogger.getInstance().log(posterId, "POST_TASK", 
                    String.format("Posted task %s (Category: %s): %s", taskId, category, description));
            System.out.printf("Task posted successfully! Task ID: %s\n", taskId);

            // Execute parallel matching asynchronously (Unit 4 Futures & Callables)
            Future<List<User>> matchFuture = matchingEngine.matchTaskAsync(task);
            
            // Run notifications in a background thread to keep CLI responsive
            new Thread(() -> {
                try {
                    List<User> matches = matchFuture.get(); // Blocks until search completes
                    // Notify matching users in parallel (Unit 4 multi-threading)
                    notificationService.notifyHelpers(task, matches);
                } catch (Exception e) {
                    System.err.println("[Async Error] Task matching or notification failed: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Failed to save task to database: " + e.getMessage());
        }
    }

    /**
     * Views all registered tasks with filtering options (Option 3).
     */
    private static void handleViewTasks() {
        System.out.println("\n--- View Tasks ---");
        System.out.println(" 1. View All Tasks");
        System.out.println(" 2. Filter by Category (Streams)");
        System.out.println(" 3. Filter by User (Poster/Helper)");
        System.out.println(" 4. Filter by Minimum Poster Trust Score (Streams)");
        int choice = ui.readInt("Select view option (1-4): ", 1, 4);

        List<Task> listToDisplay = switch (choice) {
            case 1 -> taskRegistry.getTasks();
            case 2 -> {
                System.out.println("Select Category:\n1. ERRAND\n2. SKILL\n3. BORROW\n4. TEACH");
                int catChoice = ui.readInt("Choice: ", 1, 4);
                TaskCategory cat = switch (catChoice) {
                    case 1 -> TaskCategory.ERRAND;
                    case 2 -> TaskCategory.SKILL;
                    case 3 -> TaskCategory.BORROW;
                    default -> TaskCategory.TEACH;
                };
                yield taskService.getTasksByCategory(cat);
            }
            case 3 -> {
                String userId = ui.readString("Enter User ID: ");
                User u = userRepository.findById(userId);
                if (u == null) {
                    System.out.println("User not found.");
                    yield List.of();
                }
                yield taskRegistry.getTasksByUser(u);
            }
            default -> {
                float trust = ui.readFloat("Enter minimum trust score threshold (0-100): ", 0.0f, 100.0f);
                yield taskService.getTasksByTrustThreshold(trust);
            }
        };

        ui.displayTasks(listToDisplay);
    }

    /**
     * Accepts a posted task for resolution (Option 4).
     */
    private static void handleAcceptTask() {
        System.out.println("\n--- Accept a Task ---");
        String helperId = ui.readString("Enter helper User ID: ");
        User helper = userRepository.findById(helperId);
        if (helper == null) {
            System.out.println("Error: Helper is not registered in the system.");
            return;
        }

        String taskId = ui.readString("Enter Task ID to accept: ");
        Task task = taskRegistry.findById(taskId);
        if (task == null) {
            System.out.println("Error: Task ID not found.");
            return;
        }

        if (task.getStatus() != TaskStatus.POSTED) {
            System.out.printf("Error: Cannot accept task in %s state.\n", task.getStatus());
            return;
        }

        // Set accepted helper for validation check
        task.setAcceptedBy(helper);

        // Run validation and status update using Dispatcher (Unit 2 Pattern Matching & Polymorphism)
        try {
            taskDispatcher.inspectTask(task); // inspect current status
            taskDispatcher.dispatch(task); // validates requirements and updates status to ACCEPTED

            // Check if status transitioned to ACCEPTED
            if (task.getStatus() == TaskStatus.ACCEPTED) {
                // Save status change in DB
                taskDAO.save(task);
                FileLogger.getInstance().log(helperId, "ACCEPT_TASK", "Accepted task " + taskId);
                System.out.println("Task accepted successfully! Go ahead and help your neighbor.");
            } else {
                // Reset helper if dispatch validation caught error silently
                task.setAcceptedBy(null);
                task.setStatus(TaskStatus.POSTED);
            }
        } catch (Exception e) {
            // Insufficient trust exception or self acceptance
            task.setAcceptedBy(null);
            task.setStatus(TaskStatus.POSTED);
            System.err.println("Failed to accept task: " + e.getMessage());
        }
    }

    /**
     * Marks an accepted task as completed and submits proof (Option 5).
     */
    private static void handleCompleteTask() {
        System.out.println("\n--- Complete Task ---");
        String helperId = ui.readString("Enter helper User ID: ");
        User helper = userRepository.findById(helperId);
        if (helper == null) {
            System.out.println("Error: Helper is not registered.");
            return;
        }

        String taskId = ui.readString("Enter Task ID: ");
        Task task = taskRegistry.findById(taskId);
        if (task == null) {
            System.out.println("Error: Task ID not found.");
            return;
        }

        if (task.getStatus() != TaskStatus.ACCEPTED) {
            System.out.printf("Error: Cannot complete task. Current status is: %s\n", task.getStatus());
            return;
        }

        if (!task.getAcceptedBy().getUserId().equals(helperId)) {
            System.out.println("Error: Only the assigned helper can complete this task.");
            return;
        }

        String proofText = ui.readString("Enter proof of completion notes (e.g. 'Left laundry at room door'): ");
        if (proofText.isEmpty()) {
            System.out.println("Proof note cannot be empty.");
            return;
        }

        // Complete the task in-memory (CompletableTask interface method)
        task.markComplete(proofText);
        
        try {
            taskDAO.save(task);
            FileLogger.getInstance().log(helperId, "COMPLETE_TASK", "Completed task " + taskId + ". Proof: " + proofText);
            System.out.println("Task marked completed! Waiting for poster rating/verification.");
        } catch (Exception e) {
            System.err.println("Failed to update task completion in database: " + e.getMessage());
        }
    }

    /**
     * Requester rates the helper, finalizing task and updating score via queue (Option 6).
     */
    private static void handleRateUser() {
        System.out.println("\n--- Rate Helper & Close Task ---");
        String posterId = ui.readString("Enter poster User ID: ");
        User poster = userRepository.findById(posterId);
        if (poster == null) {
            System.out.println("Error: Poster not registered.");
            return;
        }

        String taskId = ui.readString("Enter completed Task ID: ");
        Task task = taskRegistry.findById(taskId);
        if (task == null) {
            System.out.println("Error: Task ID not found.");
            return;
        }

        if (!task.getPostedBy().getUserId().equals(posterId)) {
            System.out.println("Error: Only the task poster can rate and close this task.");
            return;
        }

        if (task.getStatus() != TaskStatus.COMPLETED) {
            System.out.printf("Error: Task cannot be closed. Current status is %s (must be COMPLETED).\n", task.getStatus());
            return;
        }

        System.out.println("\nRate helper's completion (1-5 stars):");
        System.out.println(" 5 - Excellent, very reliable (+2.0 trust)");
        System.out.println(" 4 - Good job, reliable        (+1.0 trust)");
        System.out.println(" 3 - Average performance       (+0.0 trust)");
        System.out.println(" 2 - Late or poor quality      (-2.0 trust)");
        System.out.println(" 1 - Terrible / Dispute raised (-5.0 trust)");
        int ratingChoice = ui.readInt("Select rating (1-5): ", 1, 5);

        String feedback = ui.readString("Enter short feedback notes: ");

        // Submit completion proof details to proof store
        String proofId = UUID.randomUUID().toString().substring(0, 8);
        CompletionProof proof = new CompletionProof(proofId, taskId, feedback, poster, LocalDateTime.now());
        proofStore.addProof(proof);

        // Attach proof on task (CompletableTask interface method)
        task.attachProof(feedback, poster);

        try {
            taskDAO.save(task);
            
            // Produce Completion event (Unit 4 wait/notify inter-thread communication)
            interThreadComm.submitCompletionEvent(task, ratingChoice, feedback);

            System.out.println("Task closed successfully! Helper's reputation score is being updated in the background.");
            FileLogger.getInstance().log(posterId, "CLOSE_TASK", 
                    String.format("Closed task %s. Helper: %s. Rating: %d. Feedback: %s",
                            taskId, task.getAcceptedBy().getName(), ratingChoice, feedback));

        } catch (Exception e) {
            System.err.println("Failed to update database task state: " + e.getMessage());
        }
    }

    /**
     * Displays profile and trust score history (Option 7).
     */
    private static void handleViewProfile() {
        System.out.println("\n--- View Profile ---");
        String userId = ui.readString("Enter User ID: ");
        User user = userRepository.findById(userId);
        if (user == null) {
            System.out.println("Error: User not found in system.");
            return;
        }

        try {
            // Load trust history from database
            persistence.TrustHistoryDAO thDAO = new persistence.TrustHistoryDAO();
            List<models.TrustUpdateRecord<User>> history = thDAO.findByUserId(userId);
            
            ui.displayUserProfile(user, history);
        } catch (Exception e) {
            System.err.println("Error querying trust history: " + e.getMessage());
        }
    }

    /**
     * Exports system users and tasks data into CSV files (Option 8).
     */
    private static void handleExportData() {
        System.out.println("\n--- Export Data to CSV ---");
        try {
            DataExporter.exportUsers(userRepository.getAllUsers(), "users.csv");
            DataExporter.exportTasks(taskRegistry.getTasks(), "tasks.csv");
            System.out.println("Export completed successfully!");
            System.out.println(" - Users list saved to: users.csv");
            System.out.println(" - Tasks history saved to: tasks.csv");
            FileLogger.getInstance().log("SYSTEM", "EXPORT", "Exported users.csv and tasks.csv");
        } catch (Exception e) {
            System.err.println("Failed to export data: " + e.getMessage());
        }
    }

    /**
     * Populates database with sample users and tasks on the first launch of application.
     */
    private static void generateDemoData() throws Exception {
        // Check if database already has users
        if (!userDAO.findAll().isEmpty()) {
            return; // Already populated
        }

        System.out.println("--------------------------------------------------");
        System.out.println("[Demo Engine] Empty database detected.");
        System.out.println("[Demo Engine] Creating 50 sample users (A101-A550) & 20 sample tasks...");
        System.out.println("--------------------------------------------------");

        List<User> sampleUsers = new ArrayList<>();
        String[] roles = {"student", "resident", "tutor", "volunteer"};
        String[] names = {
            "Aarav", "Ananya", "Aditya", "Bhavya", "Chaitanya", "Divya", "Eshwar", "Gauri", "Harish", "Isha",
            "Karan", "Kavya", "Madhav", "Meera", "Nikhil", "Nisha", "Pranav", "Pooja", "Rahul", "Riya",
            "Siddharth", "Sneha", "Tanmay", "Uma", "Vikram", "Yash", "Zoya", "Arjun", "Aditi", "Amit",
            "Neha", "Rohan", "Shreya", "Varun", "Priya", "Kunal", "Sanjana", "Akash", "Anjali", "Deepak",
            "Kriti", "Manish", "Payal", "Rajesh", "Swati", "Suresh", "Rani", "Vijay", "Jyoti", "Sanjay"
        };

        for (int i = 0; i < 50; i++) {
            String userId = "U" + (100 + i);
            String name = names[i % names.length];
            // room distribution e.g., wing A, floor 1 to 5, room 01 to 10
            int floor = 1 + (i % 5);
            int roomNum = 1 + (i % 10);
            String room = String.format("A%d%02d", floor, roomNum); 
            String role = roles[i % roles.length];
            float score = 50.0f;

            // Vary initial scores to simulate past history
            if (i % 7 == 0) score = 65.0f;
            else if (i % 11 == 0) score = 42.0f;
            else if (i % 13 == 0) score = 55.0f;

            User u = new User(userId, name, room, role, score);
            userDAO.save(u);
            sampleUsers.add(u);
        }

        // Generate 20 Tasks
        String[] descriptions = {
            "Need someone to collect laundry from ground floor locker",
            "Java OOP programming project help - abstract factory layout",
            "Borrow a drill kit for room shelf mounting (2 hours max)",
            "Tutoring needed for electromagnetic waves midterm exams",
            "Help moving heavy luggage boxes from parking to third floor room",
            "Borrow a USB-C charging cable for tonight",
            "Explain git merge conflicts resolution workflow",
            "Pick up Amazon parcel from hostel main guard gate",
            "Borrow a scientific calculator for tomorrow morning's physics exam",
            "Need help tuning my guitar and basic string replacement",
            "Need medicines purchased from pharmacy near campus",
            "Help debug multi-threaded synchronization bug in simulation code",
            "Borrow a bicycle for 30 minutes to visit the bank",
            "Teach me fundamental SQL triggers and procedure syntax",
            "Help sorting bedsheets from student common stores",
            "Borrow a yoga mat for the weekend fitness workshop",
            "Need assistance wiping down fan blades (have step ladder)",
            "Explain database indexing and execution plans",
            "Pick up lunch from central mess for sick roommate",
            "Borrow a volleyball for evening practice"
        };

        TaskCategory[] categories = {
            TaskCategory.ERRAND, TaskCategory.SKILL, TaskCategory.BORROW, TaskCategory.TEACH,
            TaskCategory.ERRAND, TaskCategory.BORROW, TaskCategory.SKILL, TaskCategory.ERRAND,
            TaskCategory.BORROW, TaskCategory.SKILL, TaskCategory.ERRAND, TaskCategory.SKILL,
            TaskCategory.BORROW, TaskCategory.TEACH, TaskCategory.ERRAND, TaskCategory.BORROW,
            TaskCategory.ERRAND, TaskCategory.TEACH, TaskCategory.ERRAND, TaskCategory.BORROW
        };

        persistence.TrustHistoryDAO thDAO = new persistence.TrustHistoryDAO();
        for (int i = 0; i < 20; i++) {
            String taskId = "T" + (1000 + i);
            TaskCategory cat = categories[i];
            String desc = descriptions[i];
            
            // Poster is from first 15 users
            User poster = sampleUsers.get(i % 15);
            Task task = new Task(taskId, cat, desc, poster);

            // Set up some tasks in varied statuses
            if (i % 4 == 0) {
                // Accepted
                User helper = sampleUsers.get((i + 5) % 50);
                task.setAcceptedBy(helper);
                task.setStatus(TaskStatus.ACCEPTED);
            } else if (i % 3 == 0) {
                // Completed
                User helper = sampleUsers.get((i + 8) % 50);
                task.setAcceptedBy(helper);
                task.markComplete("Finished the work. Let me know.");
                task.setStatus(TaskStatus.COMPLETED);
            } else if (i % 5 == 0) {
                // Closed task with trust update history
                User helper = sampleUsers.get((i + 12) % 50);
                task.setAcceptedBy(helper);
                task.attachProof("Job is done cleanly.", poster);
                task.setCompletedAt(LocalDateTime.now().minusDays(2));
                task.setStatus(TaskStatus.CLOSED);

                // Add proof in database
                String proofId = UUID.randomUUID().toString().substring(0, 8);
                CompletionProof proof = new CompletionProof(proofId, taskId, "Job completed cleanly.", poster, LocalDateTime.now().minusDays(2));
                new persistence.CompletionProofDAO().save(proof);

                // Record trust updates
                String recordId = UUID.randomUUID().toString().substring(0, 8);
                models.TrustUpdateRecord<User> historyRecord = new models.TrustUpdateRecord<>(
                        recordId, helper, taskId, 2.0f, "Completed Borrow request",
                        LocalDateTime.now().minusDays(2), poster.getUserId()
                );
                thDAO.recordUpdate(historyRecord);
            }

            taskDAO.save(task);
        }

        System.out.println("[Demo Engine] Demo data setup finished successfully.");
    }
}
