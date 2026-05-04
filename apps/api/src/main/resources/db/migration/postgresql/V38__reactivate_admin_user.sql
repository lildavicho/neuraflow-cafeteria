set search_path to "bar_app", public;

update "users"
set "active" = true,
    "password_hash" = '$2b$10$lnSEvOtZeO7OfiG5gbsGROxMgG5012Xa7Y/fRIibDqiLqG6c3sY7y',
    "failed_login_attempts" = 0,
    "locked_until" = null,
    "updated_at" = CURRENT_TIMESTAMP
where "email" = 'admin@ucacue.edu.ec';
