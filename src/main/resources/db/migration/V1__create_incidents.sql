CREATE TABLE incidents (
    incident_id VARCHAR(50) PRIMARY KEY,
    source VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL
);