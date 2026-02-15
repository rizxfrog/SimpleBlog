-- 8.1 读取某分支最新版本（tip）
select c.*
from doc_ref r
join doc_commit c
  on c.doc_id = r.doc_id
 and c.id = r.commit_id
where r.doc_id = :doc_id
  and r.ref_name = :ref_name;

-- 8.2 列出分支
select ref_name, commit_id, update_at
from doc_ref
where doc_id = :doc_id
  and ref_type = 'branch'
order by ref_name;

-- 8.3 查看 commit 历史（first-parent 线性历史，类似 Git 的 --first-parent）
with recursive chain as (
    select c.id, c.create_at, 0 as depth
    from doc_ref r
    join doc_commit c
      on c.doc_id = r.doc_id
     and c.id = r.commit_id
    where r.doc_id = :doc_id
      and r.ref_name = :ref_name

    union all

    select p.parent_commit_id, c2.create_at, ch.depth + 1
    from chain ch
    join doc_commit_parent p
      on p.doc_id = :doc_id
     and p.child_commit_id = ch.id
     and p.parent_order = 0
    join doc_commit c2
      on c2.doc_id = p.doc_id
     and c2.id = p.parent_commit_id
    where ch.depth < :max_depth
)
select c.*
from chain ch
join doc_commit c
  on c.doc_id = :doc_id
 and c.id = ch.id
order by ch.depth;

-- 8.4 查两 commit 的最近公共祖先（LCA）
-- 这块 SQL 能写，但较长且性能要小心。工程上更常见做法：
-- 1) 服务层用 BFS/双向 BFS，把 doc_commit_parent 当边表。
-- 2) 数据库只做存储和索引。
-- 3) 需要极致性能时，再补祖先闭包/高度缓存表。

-- 10) 可以立刻加的增强（建议）
-- 软删除/回收站：doc_node 上 is_deleted 已有
-- 搜索：ES / OpenSearch
-- 权限：space/doc 级 ACL 表（user_id, role）
-- 内容去重：content_hash + 对象存储 blob 复用
