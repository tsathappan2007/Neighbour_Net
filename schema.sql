-- Database Schema for NeighbourNet

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100),
    room_no VARCHAR(20),
    role VARCHAR(50),
    trust_score FLOAT DEFAULT 50.0,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(20) PRIMARY KEY,
    category VARCHAR(20),
    description VARCHAR(500),
    status VARCHAR(20),
    posted_by VARCHAR(20),
    accepted_by VARCHAR(20),
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (posted_by) REFERENCES users(user_id),
    FOREIGN KEY (accepted_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS trust_history (
    record_id VARCHAR(20) PRIMARY KEY,
    user_id VARCHAR(20),
    task_id VARCHAR(20),
    delta FLOAT,
    reason VARCHAR(200),
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id)
);

CREATE TABLE IF NOT EXISTS completion_proofs (
    proof_id VARCHAR(20) PRIMARY KEY,
    task_id VARCHAR(20),
    note VARCHAR(500),
    verified_by VARCHAR(20),
    created_at TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    FOREIGN KEY (verified_by) REFERENCES users(user_id)
);
