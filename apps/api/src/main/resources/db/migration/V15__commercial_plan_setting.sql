INSERT INTO settings (setting_key, setting_value)
VALUES ('commercial_plan', 'START')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
