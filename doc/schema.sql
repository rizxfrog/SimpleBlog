-- PostgreSQL schema for SimpleBlog

do $$
begin
    if not exists (select 1 from pg_type where typname = 'user_status') then
        create type user_status as enum ('active', 'disabled', 'pending');
    end if;
    if not exists(select 1 from pg_type where typname = 'role_code') then
        create type role_code as enum ('admin', 'author', 'user');
    end if;
end $$;

create table if not exists roles (
    id bigserial primary key,
    code role_code,
    name varchar(64) not null
);

create table if not exists permissions (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(128) not null
);

create table if not exists role_permissions (
    role_id bigint not null references roles(id) on delete cascade,
    permission_id bigint not null references permissions(id) on delete cascade,
    primary key (role_id, permission_id)
);

create table if not exists users (
    id bigserial primary key,
    username varchar(64) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(128),
    email varchar(128),
    avatar_url varchar(512),
    status user_status default 'active',
    role_id bigint references roles(id),
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now()
);

create table if not exists categories (
    id bigserial primary key,
    name varchar(64) not null,
    slug varchar(64) not null unique
);

create table if not exists tags (
    id bigserial primary key,
    name varchar(64) not null,
    slug varchar(64) not null unique
);

create table if not exists blogs (
    id bigserial primary key,
    title varchar(200) not null,
    summary varchar(400),
    content text not null,
    author_id bigint not null references users(id),
    category_id bigint references categories(id),
    cover_url varchar(512),
    is_published boolean default false,
    views bigint default 0,
    likes bigint default 0,
    dislikes bigint default 0,
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now()
);

create table if not exists blog_tags (
    id bigserial primary key,
    blog_id bigint not null references blogs(id) on delete cascade,
    tag_id bigint not null references tags(id) on delete cascade
);

create table if not exists comments (
    id bigserial primary key,
    blog_id bigint not null references blogs(id) on delete cascade,
    user_id bigint references users(id) on delete set null,
    parent_id bigint,
    content text not null,
    status varchar(16) default 'pending',
    upvotes bigint default 0,
    downvotes bigint default 0,
    author_name varchar(128),
    author_email varchar(128),
    author_website varchar(255),
    author_ip varchar(64),
    author_ua varchar(255),
    created_at timestamp without time zone default now()
);
create table if not exists comment_votes (
    id bigserial primary key,
    comment_id bigint not null references comments(id) on delete cascade,
    user_id bigint,
    voter_ip varchar(64),
    value smallint not null,
    created_at timestamp without time zone default now(),
    unique (comment_id, user_id, value),
    unique (comment_id, voter_ip, value)
);

create table if not exists blog_votes (
    id bigserial primary key,
    blog_id bigint not null references blogs(id) on delete cascade,
    user_id bigint,
    voter_ip varchar(64),
    value smallint not null,
    created_at timestamp without time zone default now(),
    unique (blog_id, user_id),
    unique (blog_id, voter_ip)
);

create table if not exists article_pv_daily (
    id bigserial primary key,
    blog_id bigint not null references blogs(id) on delete cascade,
    day date not null,
    views bigint default 0,
    unique (blog_id, day)
);
alter table comments add column if not exists status varchar(16) default 'pending';
alter table comments add column if not exists upvotes bigint default 0;
alter table comments add column if not exists downvotes bigint default 0;
alter table comments add column if not exists author_name varchar(128);
alter table comments add column if not exists author_email varchar(128);
alter table comments add column if not exists author_website varchar(255);
alter table comments add column if not exists author_ip varchar(64);
alter table comments add column if not exists author_ua varchar(255);
alter table comments alter column user_id drop not null;
alter table blogs add column if not exists dislikes bigint default 0;

create table if not exists files (
    id bigserial primary key,
    name varchar(255) not null,
    url varchar(512) not null,
    content_type varchar(128),
    size_bytes bigint,
    created_at timestamp without time zone default now()
);

create table if not exists configs (
    id bigserial primary key,
    key varchar(64) not null unique,
    value text
);

create index if not exists idx_blogs_published_created on blogs(is_published, created_at desc);
create index if not exists idx_comments_blog on comments(blog_id);
create index if not exists idx_comments_status on comments(status);
create index if not exists idx_comment_votes_comment on comment_votes(comment_id);
create index if not exists idx_blog_votes_blog on blog_votes(blog_id);
create index if not exists idx_article_pv_daily_day on article_pv_daily(day);

-- Full-text search support (PostgreSQL)
create extension if not exists pg_trgm;

alter table blogs
    add column if not exists search_vector tsvector generated always as (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(summary, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(content, '')), 'C')
    ) stored;

create index if not exists idx_blogs_search_vector on blogs using gin (search_vector);

create index if not exists idx_blogs_search_trgm on blogs using gin (
    (coalesce(title, '') || ' ' || coalesce(summary, '') || ' ' || coalesce(content, '')) gin_trgm_ops
);

-- Document tree (ltree)

do $$ begin
    if not exists (select 1 from pg_type where typname = 'doc_node_type') then
        create type doc_node_type as enum ('folder', 'doc');
    end if;
end $$;

create extension if not exists ltree;

create table if not exists documents (
    id bigserial primary key,
    doc_version varchar(64) not null default 'default',
    parent_id bigint references documents(id) on delete cascade,
    type doc_node_type not null ,   -- folder / doc
    title varchar(200) not null,    -- 标题
    content text check (( type = 'folder' and content is null ) or (type = 'doc')),   -- 内容
    path ltree not null,
    sort_order int not null default 0,
    is_hidden boolean not null default false,
    created_by bigint references users(id),
    updated_by bigint references users(id),
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now(),
    depth int generated always as (nlevel(path)) stored,
    unique (doc_version, path),
    unique (doc_version, parent_id, sort_order)
);

create index if not exists idx_documents_path_gist on documents using gist (path);
create index if not exists idx_documents_version_parent_sort on documents (doc_version, parent_id, sort_order);
create index if not exists idx_documents_hidden on documents (is_hidden);
create index if not exists idx_documents_version on documents (doc_version);

-- alter table documents add column if not exists doc_version varchar(64) not null default 'default';

do $$
begin
    if exists (
        select 1 from information_schema.table_constraints
        where table_name = 'documents'
          and constraint_name = 'documents_path_key'
    ) then
        alter table documents drop constraint documents_path_key;
    end if;

    if exists (
        select 1 from information_schema.table_constraints
        where table_name = 'documents'
          and constraint_name = 'documents_parent_id_sort_order_key'
    ) then
        alter table documents drop constraint documents_parent_id_sort_order_key;
    end if;

    if not exists (
        select 1
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        where t.relname = 'documents'
          and c.contype = 'u'
          and pg_get_constraintdef(c.oid) = 'UNIQUE (doc_version, path)'
    ) then
        alter table documents add constraint uk_documents_version_path unique (doc_version, path);
    end if;

    if not exists (
        select 1
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        where t.relname = 'documents'
          and c.contype = 'u'
          and pg_get_constraintdef(c.oid) = 'UNIQUE (doc_version, parent_id, sort_order)'
    ) then
        alter table documents add constraint uk_documents_version_parent_sort unique (doc_version, parent_id, sort_order);
    end if;
end $$;

-- Enforce path/parent consistency via trigger
create or replace function documents_validate_path() returns trigger as $$
declare
    parent_path ltree;
    parent_depth int;
begin
    if new.parent_id is null then
        if nlevel(new.path) <> 1 then
            raise exception 'documents.path depth must be 1 for root nodes';
        end if;
    else
        select path, depth into parent_path, parent_depth
        from documents
        where id = new.parent_id;

        if parent_path is null then
            raise exception 'documents.parent_id % does not exist', new.parent_id;
        end if;

        if not (new.path <@ parent_path and nlevel(new.path) = parent_depth + 1) then
            raise exception 'documents.path % is not a direct child of parent path %', new.path, parent_path;
        end if;
    end if;

    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_documents_validate_path on documents;
create trigger trg_documents_validate_path
before insert or update of path, parent_id on documents
for each row execute function documents_validate_path();

-- Cascading delete for entire subtree using trigger
create or replace function documents_delete_subtree() returns trigger as $$
begin
    delete from documents where path <@ old.path and id <> old.id;
    return old;
end;
$$ language plpgsql;

drop trigger if exists trg_documents_delete_subtree on documents;
create trigger trg_documents_delete_subtree
after delete on documents
for each row execute function documents_delete_subtree();


-- Document revisions
create table if not exists document_revisions (
    id bigserial primary key,
    document_id bigint not null references documents(id) on delete cascade,
    doc_version varchar(64) not null default 'default',
    type doc_node_type not null,
    title varchar(200) not null,
    content text,
    path ltree not null,
    parent_id bigint,
    sort_order int not null default 0,
    is_hidden boolean not null default false,
    revision_number int not null,
    created_by bigint references users(id),
    created_at timestamp without time zone default now()
);

create index if not exists idx_document_revisions_doc on document_revisions(document_id, revision_number desc);
create index if not exists idx_document_revisions_version_doc on document_revisions(doc_version, document_id, revision_number desc);
-- alter table document_revisions add column if not exists doc_version varchar(64) not null default 'default';
