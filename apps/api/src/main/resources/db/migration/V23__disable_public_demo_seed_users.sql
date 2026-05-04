UPDATE users
SET active = 0
WHERE email IN ('admin@ucacue.edu.ec', 'cajero@ucacue.edu.ec', 'cliente@ucacue.edu.ec')
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQr7LxMOONQ4lPbGxwLZSQhv6k8HUOO';
