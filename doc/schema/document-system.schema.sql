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
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- 1) 文档空间/项目
create table if not exists doc_space(
    id bigserial primary key,
    name varchar(100) not null,
    create_at timestamptz not null default current_timestamp,
    update_at timestamptz not null default current_timestamp,
    delete_at timestamptz default null
);
comment on table doc_space is '文档空间';
comment on column doc_space.name is '文档空间名';
create trigger update_doc_space_modtime -- 更新[更新时间]
    before update on doc_space
    for each row execute function update_modified_column();

-- 2) 目录树节点（folder / doc）
-- create type doc_node_type as enum ('folder', 'doc');
create table doc_node (
      id bigserial primary key,
      space_id bigint not null references doc_space(id),
      parent_id bigint references doc_node(id),
      node_type doc_node_type not null,
      title varchar(100) not null,
      sort_key int not null default 0,  -- 排序，越小越优先
      is_deleted boolean not null default false,
      create_at timestamptz not null default now(),
      update_at timestamptz not null default now()
);
create index idx_doc_node_space_parent_sort
    on doc_node(space_id, parent_id, sort_key, id);
create trigger update_doc_node_modtime
    before update on doc_node
    for each row execute function update_modified_column();

-- 3) “文档 repo”：doc
create table doc (
    id bigserial primary key,
    node_id bigint not null unique references doc_node(id) on delete cascade,   -- 当父表中的记录被删除时，自动删除子表中所有相关的记录。
    default_branch varchar(30) not null default 'main',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create trigger update_doc_modtime
    before update on doc
    for each row execute function update_modified_column();

-- 4) Commit：doc_commit（快照）
-- 4.1 commit 主表（内容快照）
-- Markdown 直接存 content_md 最简单；如果你未来要上对象存储，就换成 content_ref + content_hash。
create table doc_commit (
    id bigserial primary key,
    doc_id bigint not null references doc(id) on delete cascade,
    -- git风格：内容寻址用hash(sha256 32 bytes)
    commit_hash bytea not null, -- 通常是对 {doc_id, parents, title, content_hash, author, message, created_at} 做一次 sha256（服务端算），保证稳定+可校验。
    author_id bigint,
    message varchar(100) not null,
    create_at timestamptz not null default now(),
    -- 内容快照
    title text not null ,
    content_md text not null,
    -- 内容hash (用于去重/对比)
    content_hash bytea generated always as ( digest(content_md, 'sha256') ) stored not null
);
create unique index uk_doc_commit_doc_hash
    on doc_commit(doc_id, commit_hash);
create index idx_doc_commit_doc_create_at
    on doc_commit(doc_id, create_at desc );

-- 5) 分支/标签：doc_ref
-- Git 的 ref 就是名字 → commit 指针。一个 doc 内可以有多个 ref。
create type doc_ref_type as enum ('branch', 'tag');
create table doc_ref (
    doc_id bigint not null references doc(id) on delete cascade,
    ref_name varchar(100) not null, -- e.g. 'refs/heads/main', 'refs/tags/v1.0'
    commit_id bigint not null references doc_commit(id) on delete set null , -- 这是“引用关系”，不是拥有关系
    ref_type doc_ref_type not null,
    update_at timestamptz not null default now(),
    primary key (doc_id, ref_name)
);
create trigger update_doc_ref_modtime -- 更新[更新时间]
    before update on doc_ref
    for each row execute function update_modified_column();
-- 约定分支: refs/heads/<name>，标签: refs/tags/<name>

-- 6) HEAD / “当前检出分支”与并发控制（可选但很实用）
-- 在 UI 上选中某分支编辑，然后 push
create table doc_head (
    doc_id bigint primary key references doc(id) on delete cascade,
    head_ref varchar(100) not null, -- e.g. 'refs/heads/main'
    ref_version bigint not null default 0, -- 用于 CAS 更新，乐观锁
    update_at timestamptz not null default now()
);


commit transaction;
