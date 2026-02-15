下面给你一套空间/文档级 ACL（权限）方案，能直接和当前文档系统模型拼起来。

目标能力：

* Space（空间）和 Doc（单文档）两级授权
* 角色：`owner / admin / editor / viewer`
* 支持“空间默认权限 + 文档覆盖”
* 写操作（commit / push / merge / refs 变更）必须校验权限

---

## 1) 角色与权限点

建议动作划分：

* 读：看目录、看文档、看 commit、看分支
* 写：commit、push、merge
* 管理：改权限、删文档、删/改 refs、force push、打 tag

| role   | read | write(commit/push/merge) | manage(perms/refs/delete) |
| ------ | ---- | ------------------------ | ------------------------- |
| owner  | ✅    | ✅                        | ✅                         |
| admin  | ✅    | ✅                        | ✅                         |
| editor | ✅    | ✅                        | ❌                         |
| viewer | ✅    | ❌                        | ❌                         |

---

## 2) 表设计（Space ACL + Doc ACL）

### 2.1 Space 成员表（空间默认权限）

```sql
create table space_member (
  space_id   bigint not null references doc_space(id) on delete cascade,
  user_id    bigint not null,
  role       text not null check (role in ('owner','admin','editor','viewer')),
  create_at  timestamptz not null default now(),
  primary key (space_id, user_id)
);

create index idx_space_member_user
  on space_member(user_id, space_id);
```

### 2.2 Doc 级覆盖权限（只在需要时写）

```sql
create table doc_acl (
  doc_id     bigint not null references doc(id) on delete cascade,
  user_id    bigint not null,
  role       text not null check (role in ('owner','admin','editor','viewer')),
  create_at  timestamptz not null default now(),
  primary key (doc_id, user_id)
);

create index idx_doc_acl_user
  on doc_acl(user_id, doc_id);
```

### 2.3 `doc.acl_mode`（已在主 schema 内置）

主 schema 已包含：

* `inherit`：继承 space_member，doc_acl 可做覆盖
* `override`：只看 doc_acl（找不到即无权限）

---

## 3) 有效权限计算函数

```sql
create or replace function effective_doc_role(p_doc_id bigint, p_user_id bigint)
returns text
language sql
stable
as $$
  select
    case
      when d.acl_mode = 'override' then da.role
      else coalesce(da.role, sm.role)
    end as role
  from doc d
  join doc_node n on n.id = d.node_id
  left join doc_acl da
    on da.doc_id = d.id and da.user_id = p_user_id
  left join space_member sm
    on sm.space_id = n.space_id and sm.user_id = p_user_id
  where d.id = p_doc_id;
$$;
```

---

## 4) 写/管理权限判断（修正 NULL 问题）

```sql
create or replace function can_doc_write(p_doc_id bigint, p_user_id bigint)
returns boolean
language sql
stable
as $$
  select coalesce(
    effective_doc_role(p_doc_id, p_user_id) in ('owner','admin','editor'),
    false
  );
$$;

create or replace function can_doc_manage(p_doc_id bigint, p_user_id bigint)
returns boolean
language sql
stable
as $$
  select coalesce(
    effective_doc_role(p_doc_id, p_user_id) in ('owner','admin'),
    false
  );
$$;
```

服务层写操作入口先做：

```sql
select can_doc_write(:doc_id, :user_id);
```

---

## 5) 目录树权限建议

建议目录树按 space 权限控制；doc 的 override 权限作为可选隐藏策略。

```sql
select n.*
from doc_node n
left join doc d on d.node_id = n.id
where n.space_id = :space_id
  and n.parent_id is not distinct from :parent_id
  and n.is_deleted = false
  and exists (
    select 1
    from space_member sm
    where sm.space_id = n.space_id
      and sm.user_id = :user_id
  )
  and (
    n.node_type = 'folder'
    or d.acl_mode <> 'override'
    or exists (
      select 1
      from doc_acl da
      where da.doc_id = d.id
        and da.user_id = :user_id
    )
  )
order by n.sort_key, n.id;
```

---

## 6) 空间级写权限（建目录/建文档）

```sql
create or replace function can_space_write(p_space_id bigint, p_user_id bigint)
returns boolean
language sql
stable
as $$
  select exists (
    select 1
    from space_member
    where space_id = p_space_id
      and user_id = p_user_id
      and role in ('owner','admin','editor')
  );
$$;
```

---

## 7) 接口行为最小约定

* Space 创建者自动写入 `space_member(owner)`
* 新 doc 默认 `acl_mode='inherit'`
* 私有 doc：设置 `acl_mode='override'`，并写入创建者 `doc_acl(owner)`
* 权限修改：
  * `inherit` 模式优先改 `space_member`
  * `override` 模式改 `doc_acl`

---

## 8) 与 Git refs 权限绑定

建议规则：

* `editor+`：commit、push、merge、创建分支（heads）
* `admin+`：删除分支、打 tag、force push、改 refs 指向

push ref 时：

* 普通快进更新：`can_doc_write`
* 删除 ref / force 更新：`can_doc_manage`
