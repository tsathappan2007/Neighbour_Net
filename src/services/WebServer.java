package services;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import exceptions.InsufficientTrustException;
import handlers.TaskDispatcher;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import models.*;
import persistence.TaskDAO;
import persistence.TrustHistoryDAO;
import persistence.UserDAO;

/**
 * Embedded HTTP & REST API Server for NeighbourNet.
 * Serves the modern web UI and provides full REST endpoints bridging
 * to Java 21 DAOs, Trust Engine, and Concurrency Services.
 */
public class WebServer {
    private final int port;
    private HttpServer server;
    private final UserRepository userRepository;
    private final TaskRegistry taskRegistry;
    private final TrustEngine<User> trustEngine;
    private final InterThreadCommunication interThreadComm;
    private final TaskMatchingEngine matchingEngine;
    private final NotificationService notificationService;
    private final TaskDispatcher taskDispatcher;
    private final TaskDAO taskDAO;
    private final UserDAO userDAO;
    private final TrustHistoryDAO trustHistoryDAO;
    private final CompletionProofStore proofStore;

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WebServer(int port,
                     UserRepository userRepository,
                     TaskRegistry taskRegistry,
                     TrustEngine<User> trustEngine,
                     InterThreadCommunication interThreadComm,
                     TaskMatchingEngine matchingEngine,
                     NotificationService notificationService,
                     TaskDispatcher taskDispatcher,
                     CompletionProofStore proofStore) {
        this.port = port;
        this.userRepository = userRepository;
        this.taskRegistry = taskRegistry;
        this.trustEngine = trustEngine;
        this.interThreadComm = interThreadComm;
        this.matchingEngine = matchingEngine;
        this.notificationService = notificationService;
        this.taskDispatcher = taskDispatcher;
        this.proofStore = proofStore;
        this.taskDAO = new TaskDAO();
        this.userDAO = new UserDAO();
        this.trustHistoryDAO = new TrustHistoryDAO();
    }

    public void start() throws IOException {
        int activePort = this.port;
        boolean started = false;
        
        while (!started && activePort <= this.port + 10) {
            try {
                server = HttpServer.create(new InetSocketAddress(activePort), 0);
                started = true;
            } catch (IOException e) {
                System.out.printf("[WebServer] Port %d in use, trying %d...\n", activePort, activePort + 1);
                activePort++;
            }
        }

        if (!started) {
            throw new IOException("Could not bind WebServer to any port in range " + this.port + "-" + (this.port + 10));
        }

        // Router
        server.createContext("/api/tasks", new TasksHandler());
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.printf("\n=======================================================\n");
        System.out.printf("  NeighbourNet Web Application Live at: http://localhost:%d\n", activePort);
        System.out.printf("=======================================================\n\n");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : port;
    }

    // ==========================================
    // Handlers
    // ==========================================

    private class TasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(method) && (path.equals("/api/tasks") || path.equals("/api/tasks/"))) {
                    List<Task> tasks = new ArrayList<>(taskRegistry.getTasks());
                    tasks.sort((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"status\":\"success\",\"count\":").append(tasks.size()).append(",\"tasks\":[");
                    for (int i = 0; i < tasks.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(taskToJson(tasks.get(i)));
                    }
                    sb.append("]}");
                    sendJsonResponse(exchange, 200, sb.toString());
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && (path.equals("/api/tasks") || path.equals("/api/tasks/"))) {
                    // Post new task
                    String body = readRequestBody(exchange);
                    Map<String, String> json = parseSimpleJson(body);

                    String posterId = json.get("posterId");
                    String categoryStr = json.get("category");
                    String description = json.get("description");

                    if (posterId == null || categoryStr == null || description == null || description.trim().isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Missing required fields (posterId, category, description)\"}");
                        return;
                    }

                    User poster = userRepository.findById(posterId);
                    if (poster == null) {
                        sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"Poster user not found with ID: " + posterId + "\"}");
                        return;
                    }

                    TaskCategory category;
                    try {
                        category = TaskCategory.valueOf(categoryStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Invalid category. Must be ERRAND, SKILL, BORROW, or TEACH.\"}");
                        return;
                    }

                    String taskId = "T" + (1000 + taskRegistry.getTasks().size() + 1);
                    Task newTask = new Task(taskId, category, description.trim(), poster);

                    // Add to memory & DB
                    taskRegistry.add(newTask);
                    taskDAO.save(newTask);

                    // Concurrency matching & notifications (Unit 4)
                    Future<List<User>> matchedFuture = matchingEngine.matchTaskAsync(newTask);
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            List<User> matched = matchedFuture.get();
                            notificationService.notifyHelpers(newTask, matched);
                        } catch (Exception ignored) {}
                    });

                    taskDispatcher.dispatch(newTask);

                    FileLogger.getInstance().log("TASK", "CREATE",
                            String.format("Task %s [%s] created by %s", taskId, category, poster.getName()));

                    sendJsonResponse(exchange, 201, "{\"status\":\"success\",\"message\":\"Task created successfully\",\"task\":" + taskToJson(newTask) + "}");
                    return;
                }

                // Sub-routes: /api/tasks/{id}/accept, /api/tasks/{id}/complete, /api/tasks/{id}/rate
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    String taskId = parts[3];
                    String action = parts.length >= 5 ? parts[4] : "";

                    Task task = taskRegistry.findById(taskId);
                    if (task == null) {
                        sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"Task not found: " + taskId + "\"}");
                        return;
                    }

                    if ("POST".equalsIgnoreCase(method) && "accept".equalsIgnoreCase(action)) {
                        String body = readRequestBody(exchange);
                        Map<String, String> json = parseSimpleJson(body);
                        String helperId = json.get("helperId");

                        if (helperId == null) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"helperId is required\"}");
                            return;
                        }

                        User helper = userRepository.findById(helperId);
                        if (helper == null) {
                            sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"Helper user not found: " + helperId + "\"}");
                            return;
                        }

                        if (task.getPostedBy().getUserId().equals(helper.getUserId())) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"You cannot accept your own task.\"}");
                            return;
                        }

                        if (task.getStatus() != TaskStatus.POSTED) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Task is not in POSTED state (current: " + task.getStatus() + ").\"}");
                            return;
                        }

                        float minRequired = getCategoryMinTrust(task.getCategory());
                        if (helper.getTrustScore() < minRequired) {
                            sendJsonResponse(exchange, 400, String.format(Locale.US,
                                    "{\"status\":\"error\",\"message\":\"Insufficient Trust Score! Category '%s' requires minimum trust score of %.1f, but your current score is %.1f.\"}",
                                    task.getCategory(), minRequired, helper.getTrustScore()
                            ));
                            return;
                        }

                        task.setAcceptedBy(helper);
                        task.setStatus(TaskStatus.ACCEPTED);
                        taskDAO.save(task);
                        taskDispatcher.dispatch(task);

                        FileLogger.getInstance().log("TASK", "ACCEPT",
                                String.format("Task %s accepted by helper %s (Trust: %.1f)", taskId, helper.getName(), helper.getTrustScore()));

                        sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Task accepted successfully!\",\"task\":" + taskToJson(task) + "}");
                        return;
                    }

                    if ("POST".equalsIgnoreCase(method) && "complete".equalsIgnoreCase(action)) {
                        String body = readRequestBody(exchange);
                        Map<String, String> json = parseSimpleJson(body);
                        String helperId = json.get("helperId");
                        String proofNote = json.get("proofNote");

                        if (task.getStatus() != TaskStatus.ACCEPTED) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Task must be in ACCEPTED state to complete.\"}");
                            return;
                        }

                        if (helperId != null && task.getAcceptedBy() != null && !task.getAcceptedBy().getUserId().equals(helperId)) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Only the assigned helper can complete this task.\"}");
                            return;
                        }

                        String note = (proofNote != null && !proofNote.trim().isEmpty()) ? proofNote.trim() : "Completed as requested.";
                        task.markComplete(note);
                        taskDAO.save(task);

                        // Persist proof
                        CompletionProof proof = new CompletionProof(
                                UUID.randomUUID().toString(),
                                task.getTaskId(),
                                note,
                                task.getPostedBy(),
                                LocalDateTime.now()
                        );
                        proofStore.addProof(proof);

                        FileLogger.getInstance().log("TASK", "COMPLETE",
                                String.format("Task %s marked completed by helper %s", taskId, task.getAcceptedBy() != null ? task.getAcceptedBy().getName() : "Helper"));

                        sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Task completed and proof attached!\",\"task\":" + taskToJson(task) + "}");
                        return;
                    }

                    if ("POST".equalsIgnoreCase(method) && "rate".equalsIgnoreCase(action)) {
                        String body = readRequestBody(exchange);
                        Map<String, String> json = parseSimpleJson(body);
                        String posterId = json.get("posterId");
                        String ratingStr = json.get("rating");
                        String feedback = json.get("feedback");

                        if (task.getStatus() != TaskStatus.COMPLETED) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Task must be in COMPLETED state to rate.\"}");
                            return;
                        }

                        if (posterId != null && !task.getPostedBy().getUserId().equals(posterId)) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Only the task poster can rate and close this task.\"}");
                            return;
                        }

                        float rating = 5.0f;
                        try {
                            if (ratingStr != null) rating = Float.parseFloat(ratingStr);
                        } catch (NumberFormatException e) {
                            rating = 5.0f;
                        }

                        if (rating < 1.0f || rating > 5.0f) {
                            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Rating must be between 1 and 5.\"}");
                            return;
                        }

                        String fb = (feedback != null && !feedback.trim().isEmpty()) ? feedback.trim() : "Rating " + rating + " stars";
                        task.attachProof(task.getProofNote() != null ? task.getProofNote() : fb, task.getPostedBy());
                        taskDAO.save(task);

                        // Enqueue rating update to InterThreadCommunication consumer (Unit 4 wait/notify)
                        interThreadComm.submitCompletionEvent(task, rating, fb);

                        FileLogger.getInstance().log("TRUST", "RATING",
                                String.format("Task %s rated %.1f stars by %s for helper %s",
                                        taskId, rating, task.getPostedBy().getName(),
                                        task.getAcceptedBy() != null ? task.getAcceptedBy().getName() : "N/A"));

                        // Brief pause to let consumer process
                        try { Thread.sleep(80); } catch (InterruptedException ignored) {}

                        sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Rating submitted, task closed, and reputation updated!\",\"task\":" + taskToJson(task) + "}");
                        return;
                    }
                }

                sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"Endpoint not found\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    String[] parts = path.split("/");
                    if (parts.length >= 4 && !parts[3].isEmpty()) {
                        String userId = parts[3];
                        User user = userRepository.findById(userId);
                        if (user == null) {
                            sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"User not found: " + userId + "\"}");
                            return;
                        }

                        List<TrustUpdateRecord<User>> history = trustHistoryDAO.findByUserId(userId);
                        StringBuilder sb = new StringBuilder();
                        sb.append("{\"status\":\"success\",\"user\":").append(userToJson(user)).append(",\"trustHistory\":[");
                        for (int i = 0; i < history.size(); i++) {
                            if (i > 0) sb.append(",");
                            TrustUpdateRecord<User> r = history.get(i);
                            sb.append("{")
                              .append("\"recordId\":\"").append(escapeJson(r.getRecordId())).append("\",")
                              .append("\"delta\":").append(String.format(Locale.US, "%.1f", r.getDelta())).append(",")
                              .append("\"reason\":\"").append(escapeJson(r.getReason())).append("\",")
                              .append("\"timestamp\":\"").append(r.getTimestamp() != null ? r.getTimestamp().format(ISO_FORMAT) : "").append("\"")
                              .append("}");
                        }
                        sb.append("]}");
                        sendJsonResponse(exchange, 200, sb.toString());
                        return;
                    }

                    // List all users
                    List<User> users = userRepository.getAllUsers();
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"status\":\"success\",\"count\":").append(users.size()).append(",\"users\":[");
                    for (int i = 0; i < users.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(userToJson(users.get(i)));
                    }
                    sb.append("]}");
                    sendJsonResponse(exchange, 200, sb.toString());
                    return;
                }

                if ("POST".equalsIgnoreCase(method)) {
                    String body = readRequestBody(exchange);
                    Map<String, String> json = parseSimpleJson(body);

                    String name = json.get("name");
                    String room = json.get("room");
                    String role = json.get("role");

                    if (name == null || name.trim().isEmpty() || room == null || room.trim().isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Name and Room are required.\"}");
                        return;
                    }

                    String userId = "U" + (100 + userRepository.getAllUsers().size() + 1);
                    String userRole = (role != null && !role.trim().isEmpty()) ? role.trim() : "Resident";
                    User newUser = new User(userId, name.trim(), room.trim(), userRole);

                    userRepository.addUser(newUser);
                    trustEngine.registerUser(newUser);

                    FileLogger.getInstance().log("USER", "REGISTER",
                            String.format("User %s (%s, Room %s) registered", userId, name, room));

                    sendJsonResponse(exchange, 201, "{\"status\":\"success\",\"message\":\"User registered successfully!\",\"user\":" + userToJson(newUser) + "}");
                    return;
                }

                sendJsonResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"Endpoint not found\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            List<Task> tasks = taskRegistry.getTasks();
            List<User> users = userRepository.getAllUsers();

            long posted = tasks.stream().filter(t -> t.getStatus() == TaskStatus.POSTED).count();
            long accepted = tasks.stream().filter(t -> t.getStatus() == TaskStatus.ACCEPTED).count();
            long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
            long closed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.CLOSED).count();

            long errandCount = tasks.stream().filter(t -> t.getCategory() == TaskCategory.ERRAND).count();
            long skillCount = tasks.stream().filter(t -> t.getCategory() == TaskCategory.SKILL).count();
            long borrowCount = tasks.stream().filter(t -> t.getCategory() == TaskCategory.BORROW).count();
            long teachCount = tasks.stream().filter(t -> t.getCategory() == TaskCategory.TEACH).count();

            double avgTrust = users.isEmpty() ? 50.0 : users.stream().mapToDouble(User::getTrustScore).average().orElse(50.0);

            String json = String.format(Locale.US,
                    "{\"status\":\"success\",\"stats\":{" +
                    "\"totalUsers\":%d," +
                    "\"totalTasks\":%d," +
                    "\"postedTasks\":%d," +
                    "\"acceptedTasks\":%d," +
                    "\"completedTasks\":%d," +
                    "\"closedTasks\":%d," +
                    "\"avgTrustScore\":%.1f," +
                    "\"errandCount\":%d," +
                    "\"skillCount\":%d," +
                    "\"borrowCount\":%d," +
                    "\"teachCount\":%d" +
                    "}}",
                    users.size(), tasks.size(), posted, accepted, completed, closed, avgTrust,
                    errandCount, skillCount, borrowCount, teachCount);

            sendJsonResponse(exchange, 200, json);
        }
    }

    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            
            if (path.contains("tasks")) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("TaskID,Category,Description,Status,PostedBy,AcceptedBy,CreatedAt,CompletedAt,ProofNote");
                for (Task t : taskRegistry.getTasks()) {
                    pw.printf("%s,%s,\"%s\",%s,%s,%s,%s,%s,\"%s\"\n",
                            t.getTaskId(),
                            t.getCategory(),
                            t.getDescription().replace("\"", "\"\""),
                            t.getStatus(),
                            t.getPostedBy() != null ? t.getPostedBy().getUserId() : "",
                            t.getAcceptedBy() != null ? t.getAcceptedBy().getUserId() : "",
                            t.getCreatedAt() != null ? t.getCreatedAt().format(ISO_FORMAT) : "",
                            t.getCompletedAt() != null ? t.getCompletedAt().format(ISO_FORMAT) : "",
                            t.getProofNote() != null ? t.getProofNote().replace("\"", "\"\"") : "");
                }
                byte[] bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"neighbournet_tasks.csv\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            if (path.contains("users")) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("UserID,Name,RoomNo,Role,TrustScore");
                for (User u : userRepository.getAllUsers()) {
                    pw.printf(Locale.US, "%s,\"%s\",%s,\"%s\",%.2f\n",
                            u.getUserId(),
                            u.getName().replace("\"", "\"\""),
                            u.getRoomNo(),
                            u.getRole().replace("\"", "\"\""),
                            u.getTrustScore());
                }
                byte[] bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"neighbournet_users.csv\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Specify /api/export/tasks or /api/export/users\"}");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.equals("/") || uriPath.isEmpty()) {
                uriPath = "/index.html";
            }

            Path publicDir = Paths.get("public").toAbsolutePath().normalize();
            Path requestedFile = publicDir.resolve(uriPath.substring(1)).normalize();

            if (!requestedFile.startsWith(publicDir) || !Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
                requestedFile = publicDir.resolve("index.html");
            }

            if (Files.exists(requestedFile) && !Files.isDirectory(requestedFile)) {
                String mimeType = getMimeType(requestedFile.toString());
                byte[] fileBytes = Files.readAllBytes(requestedFile);
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, fileBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
            } else {
                String response = "<h1>404 Not Found</h1><p>NeighbourNet web files not found in public/ folder.</p>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private float getCategoryMinTrust(TaskCategory cat) {
        return switch (cat) {
            case ERRAND -> 45.0f;
            case BORROW -> 50.0f;
            case TEACH -> 55.0f;
            case SKILL -> 60.0f;
        };
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private String taskToJson(Task t) {
        return "{" +
                "\"taskId\":\"" + escapeJson(t.getTaskId()) + "\"," +
                "\"category\":\"" + t.getCategory().name() + "\"," +
                "\"categoryDesc\":\"" + escapeJson(t.getCategory().getDescription()) + "\"," +
                "\"description\":\"" + escapeJson(t.getDescription()) + "\"," +
                "\"status\":\"" + t.getStatus().name() + "\"," +
                "\"minTrustScore\":" + String.format(Locale.US, "%.1f", getCategoryMinTrust(t.getCategory())) + "," +
                "\"postedBy\":" + (t.getPostedBy() != null ? userToJson(t.getPostedBy()) : "null") + "," +
                "\"acceptedBy\":" + (t.getAcceptedBy() != null ? userToJson(t.getAcceptedBy()) : "null") + "," +
                "\"createdAt\":\"" + (t.getCreatedAt() != null ? t.getCreatedAt().format(ISO_FORMAT) : "") + "\"," +
                "\"completedAt\":\"" + (t.getCompletedAt() != null ? t.getCompletedAt().format(ISO_FORMAT) : "") + "\"," +
                "\"proofNote\":\"" + (t.getProofNote() != null ? escapeJson(t.getProofNote()) : "") + "\"," +
                "\"verified\":" + t.isVerified() +
                "}";
    }

    private String userToJson(User u) {
        int floor = 1;
        try {
            if (u.getRoomNo() != null && u.getRoomNo().length() >= 2) {
                String digit = u.getRoomNo().replaceAll("[^0-9]", "");
                if (!digit.isEmpty()) {
                    floor = Character.getNumericValue(digit.charAt(0));
                }
            }
        } catch (Exception ignored) {}

        return "{" +
                "\"userId\":\"" + escapeJson(u.getUserId()) + "\"," +
                "\"name\":\"" + escapeJson(u.getName()) + "\"," +
                "\"roomNo\":\"" + escapeJson(u.getRoomNo()) + "\"," +
                "\"role\":\"" + escapeJson(u.getRole()) + "\"," +
                "\"trustScore\":" + String.format(Locale.US, "%.1f", u.getTrustScore()) + "," +
                "\"floor\":" + floor +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Map<String, String> parseSimpleJson(String jsonStr) {
        Map<String, String> map = new HashMap<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) return map;
        String trimmed = jsonStr.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        boolean inQuote = false;
        StringBuilder curKey = new StringBuilder();
        StringBuilder curVal = new StringBuilder();
        boolean parsingKey = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote) {
                if (c == ':') {
                    parsingKey = false;
                    continue;
                }
                if (c == ',') {
                    if (curKey.length() > 0) {
                        map.put(curKey.toString().trim(), curVal.toString().trim());
                    }
                    curKey = new StringBuilder();
                    curVal = new StringBuilder();
                    parsingKey = true;
                    continue;
                }
            }
            if (parsingKey) {
                curKey.append(c);
            } else {
                curVal.append(c);
            }
        }
        if (curKey.length() > 0) {
            map.put(curKey.toString().trim(), curVal.toString().trim());
        }
        return map;
    }

    private String getMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".csv")) return "text/csv; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }
}
