下面给你一套**空间/文档级 ACL（权限）**的“生产可用”做法，跟你这个 **Git 分支合并 + Markdown 文档 repo** 的模型是能直接拼起来的。

目标能力（常见需求）：

* Space（空间）和 Doc（单文档）两级授权
* 角色：`owner / admin / editor / viewer`
* 支持“空间默认权限 + 文档覆盖（更细粒度）”
* 写操作（commit / push / merge / 创建分支/标签）必须校验权限

---

## 1) 角色与权限点怎么定

建议先定清楚你需要的动作：

* 读：看目录、看文档、拉取 commit、看分支
* 写：提交 commit、push 更新分支、merge
* 管理：创建/删除分支标签、修改权限、删除文档、移动目录

一个简单但够用的映射：

| role   | read | write(commit/push/merge) | manage(perms/refs/delete) |
| ------ | ---- | ------------------------ | ------------------------- |
| owner  | ✅    | ✅                        | ✅                         |
| admin  | ✅    | ✅                        | ✅                         |
| editor | ✅    | ✅                        | ❌                         |
| viewer | ✅    | ❌                        | ❌                         |

---

## 2) 表设计（Space ACL + Doc ACL）

### 2.1 Space 成员表（空间级默认权限）

```sql
create table space_member (
  space_id   bigint not null references doc_space(id) on delete cascade,
  user_id    bigint not null,
  role       text not null check (role in ('owner','admin','editor','viewer')),
  created_at timestamptz not null default now(),
  primary key (space_id, user_id)
);

create index idx_space_member_user
  on space_member(user_id, space_id);
```

### 2.2 Doc 级覆盖权限（只在需要时写入）

doc 级权限只用于“覆盖空间权限”（例如：空间里都是 editor，但某个 doc 只允许 viewer）。

```sql
create table doc_acl (
  doc_id     bigint not null references doc(id) on delete cascade,
  user_id    bigint not null,
  role       text not null check (role in ('owner','admin','editor','viewer')),
  created_at timestamptz not null default now(),
  primary key (doc_id, user_id)
);

create index idx_doc_acl_user
  on doc_acl(user_id, doc_id);
```

### 2.3 doc 是否启用独立 ACL（可选但很实用）

如果你希望：**默认继承 space 权限**，只有打开“独立权限”时才看 doc_acl：

```sql
alter table doc
add column acl_mode text not null default 'inherit'
check (acl_mode in ('inherit','override'));
```

* `inherit`：完全继承 space_member
* `override`：只看 doc_acl（典型：私有文档）

---

## 3) 权限计算：一个 SQL 函数（推荐）

你需要一个“最终有效角色”的计算逻辑：

* 如果 `doc.acl_mode = 'override'`：只看 doc_acl（找不到就无权限）
* 否则（inherit）：doc_acl 优先（覆盖），找不到再退回 space_member

我们做一个函数：返回最终 role（没有返回 null）。

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

## 4) 权限校验：写操作怎么拦（commit/push/merge）

在你的服务里（Java/Go）调用 `effective_doc_role()`，做简单判断：

* `role in ('owner','admin','editor')` 才能写
* `role in ('owner','admin')` 才能 manage

如果你希望“数据库层强制”（更硬核），你可以加一个辅助函数：

```sql
create or replace function can_doc_write(p_doc_id bigint, p_user_id bigint)
returns boolean
language sql
stable
as $$
  select effective_doc_role(p_doc_id, p_user_id) in ('owner','admin','editor');
$$;

create or replace function can_doc_manage(p_doc_id bigint, p_user_id bigint)
returns boolean
language sql
stable
as $$
  select effective_doc_role(p_doc_id, p_user_id) in ('owner','admin');
$$;
```

然后你的写接口事务开始时先：

```sql
select can_doc_write(:doc_id, :user_id);
```

false 就直接拒绝。

---

## 5) 目录树权限（space 级即可）

目录树一般按 space 权限控制即可（否则你会陷入“节点级 ACL”复杂度）。做法：

* 只要是 space_member 且 role != null，就能看该 space 的目录树
* 如果 doc 有 override 且你没有 doc_acl，就隐藏该 doc 节点（可选）

隐藏 override doc 的 SQL（列子节点时过滤）：

```sql
select n.*
from doc_node n
left join doc d on d.node_id = n.id
where n.space_id = :space_id
  and n.parent_id is not distinct from :parent_id
  and n.is_deleted = false
  and (
    n.node_type = 'folder'
    or d.acl_mode <> 'override'
    or exists (select 1 from doc_acl da where da.doc_id = d.id and da.user_id = :user_id)
  )
order by n.sort_key, n.id;
```

---

## 6) “创建文档/创建文件夹”谁能做？

建议：

* space `editor+` 能创建 doc / folder
* space `admin+` 能删除/移动大范围结构、改权限

所以你还会需要 `can_space_write(space_id, user_id)`：

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

## 7) 最小可用的接口行为（你实现起来会顺）

* **Space 创建者** → 自动写入 `space_member(owner)`
* **新 doc** → `doc.acl_mode='inherit'`（默认继承空间）
* 想做“私有文档” → 把 `acl_mode='override'`，并写入 `doc_acl(owner)` 给创建者
* 修改权限：

    * `inherit` 模式：只改 space_member（全局）
    * `override` 模式：只改 doc_acl（局部）

---

## 8) 和 Git refs 权限怎么绑定？

一般：

* `editor+`：允许 commit、push、merge、创建分支（heads）
* `admin+`：允许删除分支、打 tag、强制回退（force update refs）

你 push ref 的时候做：

* 普通更新：`can_doc_write`
* 删除 ref / force push：`can_doc_manage`

---