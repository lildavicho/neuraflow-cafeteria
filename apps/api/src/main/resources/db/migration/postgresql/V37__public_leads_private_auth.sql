set search_path to "bar_app", public;

create table if not exists public_leads (
    id bigserial primary key,
    full_name varchar(180) not null,
    company_name varchar(180),
    email varchar(255) not null,
    phone varchar(50),
    city varchar(120),
    business_type varchar(120),
    branch_count integer,
    interest varchar(120),
    message text,
    status varchar(40) not null default 'NEW',
    source varchar(80) not null default 'LANDING',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_public_leads_status_created
    on public_leads (status, created_at desc);

create index if not exists idx_public_leads_email
    on public_leads (email);
