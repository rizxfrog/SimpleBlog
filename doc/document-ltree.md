# Document Tree (PostgreSQL ltree)

This document describes the database design for the Document feature with high-performance tree queries using PostgreSQL `ltree`.

## Goals

- Fast subtree and ancestor queries.
- Simple ordering among siblings.
- Support hidden nodes and audit fields.

## Extension

```sql
create extension if not exists ltree;
```

## Table: documents

```sql
create table if not exists documents (
    id bigserial primary key,
    title varchar(200) not null,
    content text,
    path ltree not null,
    parent_id bigint references documents(id) on delete restrict,
    sort_order int not null default 0,
    is_hidden boolean not null default false,
    created_by bigint references users(id),
    updated_by bigint references users(id),
    created_at timestamp without time zone default now(),
    updated_at timestamp without time zone default now(),
    depth int generated always as (nlevel(path)) stored,
    unique (path)
);
```

### Field Notes

- `path`: Full tree path encoded as `ltree`. Example: `docs.install.quickstart`.
- `parent_id`: Nullable for root nodes. Still useful for sibling ordering and integrity checks in the app layer.
- `sort_order`: Ordering within the same parent.
- `is_hidden`: Hidden nodes are excluded for non-admin users.
- `depth`: Stored computed depth for quick filtering without re-calc.

## Indexes

```sql
create index if not exists idx_documents_path_gist on documents using gist (path);
create index if not exists idx_documents_parent_sort on documents (parent_id, sort_order);
create index if not exists idx_documents_hidden on documents (is_hidden);
```

- `path` GiST index supports subtree and ancestor queries.
- `(parent_id, sort_order)` supports sibling lists.

## Query Patterns

- Subtree of a node:

```sql
select * from documents where path <@ 'docs.install';
```

- Ancestors of a node:

```sql
select * from documents where path @> 'docs.install.quickstart' order by depth asc;
```

- Direct children of a node:

```sql
select * from documents where parent_id = :id order by sort_order asc;
```

## Write Rules (App Layer)

- New child path: `parent.path || '.' || slug` (slug is normalized segment).
- Move node: update its `path` and all descendants with prefix replacement.
- On delete: either restrict when children exist or delete subtree explicitly.

## Naming Constraints

- `ltree` labels must match `[A-Za-z0-9_]+`.
- If you need hyphens, map `-` to `_` or use a slug transform.

## Migration Notes

- `parent_id` and `path` must be consistent; enforce in service logic.
- Consider adding application-level validation for unique sibling slugs.
