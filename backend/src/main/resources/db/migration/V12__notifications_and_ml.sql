-- ========================================
-- V12: Notificaciones en tiempo real + ML
-- ========================================

SET @db := DATABASE();

-- Ordenes: timestamps para preparacion/ETA (idempotente)
SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'orders' AND column_name = 'preparation_start_at'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN preparation_start_at DATETIME NULL'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'orders' AND column_name = 'estimated_ready_at'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN estimated_ready_at DATETIME NULL'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'orders' AND column_name = 'actual_ready_at'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN actual_ready_at DATETIME NULL'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Logs de notificaciones
CREATE TABLE IF NOT EXISTS notification_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NULL,
  user_id BIGINT NULL,
  notification_type VARCHAR(40) NOT NULL,
  delivery_method VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivered_at DATETIME NULL,
  error_message TEXT,
  CONSTRAINT fk_notification_logs_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_notification_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
  INDEX idx_notification_logs_order (order_id),
  INDEX idx_notification_logs_user (user_id),
  INDEX idx_notification_logs_sent_at (sent_at)
) ENGINE=InnoDB;

-- Tokens FCM: extender tabla existente (idempotente)
SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'push_tokens' AND column_name = 'device_name'
    ),
    'SELECT 1',
    'ALTER TABLE push_tokens ADD COLUMN device_name VARCHAR(255) NULL'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'push_tokens' AND column_name = 'last_used'
    ),
    'SELECT 1',
    'ALTER TABLE push_tokens ADD COLUMN last_used DATETIME NULL'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE table_schema = @db AND table_name = 'push_tokens' AND column_name = 'is_active'
    ),
    'SELECT 1',
    'ALTER TABLE push_tokens ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Predicciones ML
CREATE TABLE IF NOT EXISTS ml_predictions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NULL,
  prediction_type VARCHAR(40) NOT NULL,
  predicted_value VARCHAR(255) NOT NULL,
  actual_value VARCHAR(255),
  confidence_score DECIMAL(5,4),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ml_predictions_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_ml_predictions_order (order_id),
  INDEX idx_ml_predictions_created (created_at)
) ENGINE=InnoDB;

-- Preferencias de usuario (ML)
CREATE TABLE IF NOT EXISTS user_preferences (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  preferred_categories JSON,
  dietary_restrictions JSON,
  average_order_value DECIMAL(12,2),
  favorite_dishes JSON,
  last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE KEY unique_user_preferences (user_id)
) ENGINE=InnoDB;

-- Metrica de modelos
CREATE TABLE IF NOT EXISTS model_metrics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  model_name VARCHAR(255) NOT NULL,
  metric_type VARCHAR(20) NOT NULL,
  metric_value DECIMAL(10,4) NOT NULL,
  training_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_points_used INT,
  INDEX idx_model_metrics_name (model_name),
  INDEX idx_model_metrics_training (training_date)
) ENGINE=InnoDB;
