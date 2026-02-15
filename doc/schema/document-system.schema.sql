-- 概念映射
-- space：文档空间/项目
-- node：目录树节点（folder / doc）
-- doc：一个“文档 repo”（一个文件的版本史）
-- commit：一次提交（内容快照 + 元数据）
-- commit_parent：DAG 边（一个 commit 可有多个 parent，用于 merge）
-- ref：分支/标签指针（例如 refs/heads/main、refs/tags/v1.0）
-- head：某 doc 当前“检出”的分支（可选，偏工作区语义）
-- diff：一般不存（需要时计算），也可存 patch（可选）

start transaction isolation level serializable;

create extension if not exists pgcrypto;

-- 复用 update_at 字段的通用触发器
create or replace function update_modified_column()
returns trigger as $$
begin
    new.update_at = current_timestamp;
    return new;
end;
$$ language plpgsql;

do $$
begin
    if not exists (select 1 from pg_type where typname = 'doc_node_type') then
        create type doc_node_type as enum ('folder', 'doc');
    end if;
    if not exists (select 1 from pg_type where typname = 'doc_ref_type') then
        create type doc_ref_type as enum ('branch', 'tag');
    end if;
end $$;

-- 1) 文档空间/项目
create table doc_space(
    id bigserial primary key,
    name varchar(100) not null,
    create_at timestamptz not null default current_timestamp,
    update_at timestamptz not null default current_timestamp,
    delete_at timestamptz default null
);
comment on table doc_space is '文档空间';
comment on column doc_space.name is '文档空间名';
drop trigger if exists update_doc_space_modtime on doc_space;
create trigger update_doc_space_modtime
    before update on doc_space
    for each row execute function update_modified_column();

-- 2) 目录树节点（folder / doc）
create table doc_node (
    id bigserial primary key,
    space_id bigint not null references doc_space(id) on delete cascade,
    parent_id bigint,
    node_type doc_node_type not null,
    title varchar(100) not null,
    sort_key int not null default 0,  -- 排序，越小越优先
    is_deleted boolean not null default false,
    create_at timestamptz not null default now(),
    update_at timestamptz not null default now(),
    unique (space_id, id),
    check (parent_id is null or parent_id <> id)
);
alter table doc_node drop constraint if exists doc_node_parent_id_fkey;
alter table doc_node drop constraint if exists fk_doc_node_parent_in_space;
alter table doc_node
    add constraint fk_doc_node_parent_in_space
    foreign key (space_id, parent_id) references doc_node(space_id, id);

create index if not exists idx_doc_node_space_parent_sort
    on doc_node(space_id, parent_id, sort_key, id);
drop trigger if exists update_doc_node_modtime on doc_node;
create trigger update_doc_node_modtime
    before update on doc_node
    for each row execute function update_modified_column();

-- 3) 文档 repo：doc
create table doc (
    id bigserial primary key,
    node_id bigint not null unique references doc_node(id) on delete cascade,
    default_branch varchar(100) not null default 'refs/heads/main',
    acl_mode text not null default 'inherit'
        check (acl_mode in ('inherit', 'override')),
    create_at timestamptz not null default now(),
    update_at timestamptz not null default now()
);
drop trigger if exists update_doc_modtime on doc;
create trigger update_doc_modtime
    before update on doc
    for each row execute function update_modified_column();

create or replace function ensure_doc_node_type_doc()
returns trigger as $$
begin
    if not exists (
        select 1
        from doc_node n
        where n.id = new.node_id
          and n.node_type = 'doc'
    ) then
        raise exception 'doc.node_id % must reference doc_node(node_type=''doc'')', new.node_id;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists check_doc_node_type on doc;
create trigger check_doc_node_type
    before insert or update of node_id on doc
    for each row execute function ensure_doc_node_type_doc();

-- 4) Commit：doc_commit（快照）
create table doc_commit (
    id bigserial primary key,
    doc_id bigint not null references doc(id) on delete cascade,
    commit_hash bytea not null check (octet_length(commit_hash) = 32),
    author_id bigint,
    message varchar(100) not null,
    create_at timestamptz not null default now(),
    title text not null,
    content_md text not null,
    content_hash bytea generated always as (digest(content_md, 'sha256')) stored not null,
    constraint uk_doc_commit_doc_id_id unique (doc_id, id)
);
create unique index if not exists uk_doc_commit_doc_hash
    on doc_commit(doc_id, commit_hash);
create index if not exists idx_doc_commit_doc_create_at
    on doc_commit(doc_id, create_at desc, id desc);

-- 4.1) commit DAG 边：doc_commit_parent
create table doc_commit_parent (
    doc_id bigint not null,
    child_commit_id bigint not null,
    parent_commit_id bigint not null,
    parent_order smallint not null check (parent_order >= 0),
    create_at timestamptz not null default now(),
    primary key (doc_id, child_commit_id, parent_order),
    unique (doc_id, child_commit_id, parent_commit_id),
    constraint chk_doc_commit_parent_not_self
        check (child_commit_id <> parent_commit_id),
    constraint fk_doc_commit_parent_child
        foreign key (doc_id, child_commit_id) references doc_commit(doc_id, id) on delete cascade,
    constraint fk_doc_commit_parent_parent
        foreign key (doc_id, parent_commit_id) references doc_commit(doc_id, id) on delete cascade
);
create index if not exists idx_doc_commit_parent_parent
    on doc_commit_parent(doc_id, parent_commit_id, child_commit_id);

-- 5) 分支/标签：doc_ref
-- create type doc_ref_type as enum ('branch', 'tag');
create table doc_ref (
    doc_id bigint not null references doc(id) on delete cascade,
    ref_name varchar(100) not null, -- e.g. refs/heads/main, refs/tags/v1.0
    commit_id bigint not null,
    ref_type doc_ref_type not null,
    update_at timestamptz not null default now(),
    primary key (doc_id, ref_name),
    constraint fk_doc_ref_commit
        foreign key (doc_id, commit_id) references doc_commit(doc_id, id) on delete cascade,
    constraint chk_doc_ref_name_and_type
        check (
            (ref_type = 'branch' and ref_name ~ '^refs/heads/.+')
            or
            (ref_type = 'tag' and ref_name ~ '^refs/tags/.+')
        )
);
create index if not exists idx_doc_ref_doc_type_name
    on doc_ref(doc_id, ref_type, ref_name);
drop trigger if exists update_doc_ref_modtime on doc_ref;
create trigger update_doc_ref_modtime
    before update on doc_ref
    for each row execute function update_modified_column();

-- 6) HEAD / 当前检出分支与并发控制（可选但很实用）
create table doc_head (
    doc_id bigint primary key references doc(id) on delete cascade,
    head_ref varchar(100) not null, -- e.g. refs/heads/main
    ref_version bigint not null default 0, -- 用于 CAS 更新，乐观锁
    update_at timestamptz not null default now(),
    constraint fk_doc_head_ref
        foreign key (doc_id, head_ref) references doc_ref(doc_id, ref_name) on delete cascade,
    constraint chk_doc_head_branch_ref
        check (head_ref ~ '^refs/heads/.+')
);
drop trigger if exists update_doc_head_modtime on doc_head;
create trigger update_doc_head_modtime
    before update on doc_head
    for each row execute function update_modified_column();

commit transaction;