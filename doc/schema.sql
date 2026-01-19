-- PostgreSQL schema for SimpleBlog

create table if not exists users (
    id bigserial primary key,
    username varchar(64) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(128),
    email varchar(128),
    avatar_url varchar(512),
    status varchar(32) default 'active',
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now()
);

create table if not exists roles (
    id bigserial primary key,
    code varchar(32) not null unique,
    name varchar(64) not null
);

create table if not exists permissions (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(128) not null
);

create table if not exists user_roles (
    user_id bigint not null references users(id) on delete cascade,
    role_id bigint not null references roles(id) on delete cascade,
    primary key (user_id, role_id)
);

create table if not exists role_permissions (
    role_id bigint not null references roles(id) on delete cascade,
    permission_id bigint not null references permissions(id) on delete cascade,
    primary key (role_id, permission_id)
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
    user_id bigint not null references users(id) on delete cascade,
    parent_id bigint,
    content text not null,
    created_at timestamp without time zone default now()
);

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
