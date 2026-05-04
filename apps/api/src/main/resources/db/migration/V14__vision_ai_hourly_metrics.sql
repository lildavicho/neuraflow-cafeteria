CREATE TABLE IF NOT EXISTS cameras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    rtsp_url VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    location VARCHAR(50),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    stream_path VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

ALTER TABLE cameras
    ADD COLUMN IF NOT EXISTS created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE cameras
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS vision_detection_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    event_key VARCHAR(120) NOT NULL,
    ts DATETIME NOT NULL,
    people_count INT NOT NULL DEFAULT 0,
    unique_people INT NOT NULL DEFAULT 0,
    event_type VARCHAR(30) NOT NULL DEFAULT 'ENTRY',
    source VARCHAR(40) NOT NULL DEFAULT 'VISION_AI',
    track_ids TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vision_detection_events_camera FOREIGN KEY (camera_id) REFERENCES cameras(id),
    UNIQUE KEY uk_vision_detection_events_camera_event (camera_id, event_key),
    INDEX idx_vision_detection_events_ts (ts),
    INDEX idx_vision_detection_events_camera_ts (camera_id, ts)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vision_hourly_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    hour_start DATETIME NOT NULL,
    unique_people INT NOT NULL DEFAULT 0,
    event_count INT NOT NULL DEFAULT 0,
    last_event_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vision_hourly_metrics_camera FOREIGN KEY (camera_id) REFERENCES cameras(id),
    UNIQUE KEY uk_vision_hourly_metrics_camera_hour (camera_id, hour_start),
    INDEX idx_vision_hourly_metrics_hour_start (hour_start),
    INDEX idx_vision_hourly_metrics_camera_hour_start (camera_id, hour_start)
) ENGINE=InnoDB;
