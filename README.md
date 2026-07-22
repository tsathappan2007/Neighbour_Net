# NeighbourNet - Hyperlocal Task and Help Exchange System

NeighbourNet is a hyperlocal peer-to-peer micro-task and resource-sharing platform designed for apartments and hostels. Residents can post help requests (errands, skilled tasks, borrowing tools, teaching), and nearby neighbors are matched to resolve them. The system incorporates a trust-based reputation engine that adjusts scoring dynamically after every interaction.

The system is built as a complete, production-ready, console-based Java 21 application. It demonstrates all five units of a standard advanced Java programming curriculum end-to-end.

---

## Technical Stack & Architecture

- **Language:** Java 21 (uses switch pattern matching, guard clauses, record-like patterns)
- **Database:** SQLite (file-based relational database stored in [neighbournet.db](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/data/neighbournet.db))
- **JDBC Driver:** SQLite JDBC (stored in `lib/`)
- **No external frameworks:** Built entirely using the Java Standard Library and native JDBC.
- **Design Pattern:** Data Access Object (DAO) pattern separating data access from services and core models.

---

## Course Units Demonstration Map

The following table lists the five Java course units and points to the specific classes and methods demonstrating them in the codebase:

| Java Unit | Topics Covered | Key File Reference | Implementation Detail |
| :--- | :--- | :--- | :--- |
| **Unit 1** | OOP, Encap, Functional Programming, Streams | [User.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/models/User.java) <br> [TaskHandler.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/handlers/TaskHandler.java) <br> [TaskService.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/TaskService.java) | Private fields & getters; `@FunctionalInterface` lambda targets; Streams API to filter tasks, map elements, and run parallelStream matching. |
| **Unit 2** | Inheritance, Interfaces, Pattern Matching | [TaskHandler.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/handlers/TaskHandler.java) <br> [ErrandHandler.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/handlers/ErrandHandler.java) <br> [CompletableTask.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/handlers/CompletableTask.java) <br> [TaskDispatcher.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/handlers/TaskDispatcher.java) | Polymorphic task category validation; CompletableTask behavior modeling; Java 21 Switch Pattern Matching over Object Types (`Task`) with `when` guard clauses. |
| **Unit 3** | Custom Exceptions & I/O Streams | [InsufficientTrustException.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/exceptions/InsufficientTrustException.java) <br> [FileLogger.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/FileLogger.java) <br> [DataExporter.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/DataExporter.java) <br> [ConsoleUI.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/ui/ConsoleUI.java) | Specialized typed custom exceptions; `BufferedReader` console scanning; `FileWriter`, `BufferedWriter`, and `PrintWriter` for logger file creation and CSV data exporter. |
| **Unit 4** | Generics, Multi-threading & Concurrency | [TrustEngine.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/TrustEngine.java) <br> [NotificationService.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/NotificationService.java) <br> [TaskMatchingEngine.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/TaskMatchingEngine.java) <br> [InterThreadCommunication.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/InterThreadCommunication.java) | Generic class `<T extends User>` for flexible user types; Synchronized thread safety blocks; `NotificationThread` extending Thread; `ExecutorService` matching tasks in parallel; raw `wait`/`notifyAll` message queue consumer. |
| **Unit 5** | Java Collections Framework & JDBC | [TaskRegistry.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/TaskRegistry.java) <br> [TrustScoreCache.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/services/TrustScoreCache.java) <br> [DatabaseConnection.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/persistence/DatabaseConnection.java) <br> [UserDAO.java](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/src/persistence/UserDAO.java) | `LinkedHashSet` preserving task insertion FIFO order; `ConcurrentHashMap` caching scores; `ArrayList` database synchronization cache; Singleton Database Connection; CRUD JDBC PreparedStatements and ResultSets mapping. |

---

## How to Compile and Run

### Prerequisites
- JDK 21 or higher installed and added to your system's `PATH`.

### Option A: Using the Launch Script (Windows)
Double-click [run.bat](file:///c:/Users/S.M.Suhail%20Akthar/Desktop/Documents/CIT%20Chennai/Projects/NeighborNet/run.bat) or run it in Command Prompt:
```cmd
run.bat
```
This script cleans the build directory, compiles all source code including the SQLite JAR in the classpath, and launches the application.

### Option B: Compiling and Running Manually via Command Line
Run the following commands in the root workspace directory:

1. **Compile:**
   ```bash
   mkdir bin
   javac -d bin -cp "lib/*" src/models/*.java src/exceptions/*.java src/handlers/*.java src/persistence/*.java src/services/*.java src/ui/*.java src/NeighbourNet.java
   ```

2. **Run:**
   ```bash
   java -cp "bin;lib/*" NeighbourNet
   ```

---

## Demo Walkthrough Guide (Deterministic Review Scenario)

> [!NOTE]
> On the first run, the database initializes a demo dataset: **50 synthetic users** (in Rooms `A101` to `A550`) and **20 sample tasks** (some are pre-accepted, completed, or closed to populate trust score variations).

To test the end-to-end functionality, perform the following steps in the console menu:

1. **View Available Tasks:**
   - Choose option **3** (`View Available Tasks`) and select **1** (`View All Tasks`).
   - Note down an active `POSTED` task (e.g., `T1001` - Java Help posted by poster `U101` or similar).

2. **Accept the Task:**
   - Choose option **4** (`Accept Task`).
   - Enter a helper User ID who is NOT the poster (e.g., `U120`, trust score starts at 50.0).
   - Enter the Task ID you noted (e.g., `T1001`).
   - The system checks if the helper has the required trust score for that category (e.g., SKILL requires trust score >= 60.0). If they do not, it throws `InsufficientTrustException`.
   - *Tip:* Try using `U100` (which has a trust score of 65.0, because of demo setup) to accept a SKILL task, or use an ERRAND task (`T1000` or `T1004` which requires only 45.0 trust) with `U120`.
   - On success, the task dispatcher logs the transition and notifies nearby floor/block neighbors in the background via multithreaded notification.

3. **Complete the Task:**
   - Choose option **5** (`Complete Task & Attach Proof`).
   - Enter the helper User ID (e.g., `U100`).
   - Enter the Task ID (e.g., `T1001`).
   - Provide a proof note (e.g. `"Finished coding the layout structures and sent GitHub repo"`).
   - The status updates to `COMPLETED`.

4. **Rate User (Finalize & Update Score):**
   - Choose option **6** (`Rate Helper (Update Trust Score)`).
   - Enter the poster User ID (e.g., `U101`).
   - Enter the Completed Task ID (e.g., `T1001`).
   - Choose rating **5** (Excellent, +2.0 trust) and leave feedback notes.
   - The task transitions to `CLOSED` and the proof is attached. The rating is submitted to the asynchronous `InterThreadCommunication` queue. In the background, the consumer thread updates the helper's trust score in memory and persists the change to the `users` and `trust_history` tables.

5. **Verify Reputation Score Update:**
   - Choose option **7** (`View User Profile & History`).
   - Enter the helper User ID (e.g., `U100`).
   - You will see their profile showing their updated reputation score and the logged trust update history record.

6. **Export Data:**
   - Choose option **8** (`Export System Data (CSV)`).
   - The system writes all users to `users.csv` and tasks to `tasks.csv` using file streams. Check your root folder for these outputs.
