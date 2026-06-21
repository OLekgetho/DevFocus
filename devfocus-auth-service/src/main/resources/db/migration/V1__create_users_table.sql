CREATE SCHEMA IF NOT EXISTS devfocus_auth;

BEGIN;

CREATE TABLE devfocus_auth.users(
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    cognito_sub text not null unique ,
    github_id bigint not null unique ,
    github_username text not null ,
    github_access_token text,
    avatar_url text,
    email text,
    is_first_login boolean not null ,
    last_seen_at timestamptz
);

COMMIT;
