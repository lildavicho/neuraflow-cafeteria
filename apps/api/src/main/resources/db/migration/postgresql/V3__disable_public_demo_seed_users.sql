set search_path to "bar_app", public;

update "users"
set "active" = false,
    "updated_at" = CURRENT_TIMESTAMP
where "email" in ('admin@ucacue.edu.ec', 'cajero@ucacue.edu.ec', 'cliente@ucacue.edu.ec')
  and "password_hash" = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQr7LxMOONQ4lPbGxwLZSQhv6k8HUOO';
