set search_path to "bar_app", public;

drop index if exists "idx_users_email";
create unique index if not exists "uk_users_email_ci" on "users" ((lower("email")));
