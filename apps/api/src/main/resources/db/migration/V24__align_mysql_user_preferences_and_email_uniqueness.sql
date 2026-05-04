SET @db := DATABASE();

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @db AND table_name = 'users' AND index_name = 'idx_users_email'
    ),
    'DROP INDEX idx_users_email ON users',
    'SELECT 1'
  )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE users
  ADD UNIQUE KEY uk_users_email (email);

ALTER TABLE user_preferences
  MODIFY COLUMN preferred_categories TEXT NULL,
  MODIFY COLUMN dietary_restrictions TEXT NULL,
  MODIFY COLUMN favorite_dishes TEXT NULL;
