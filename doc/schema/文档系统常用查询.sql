-- 8.1 读取某分支最新版本（tip）
select c.*
from doc_ref r
         join doc_commit c on c.id = r.commit_id
where r.doc_id = :doc_id and r.ref_name = :ref_name;
-- 8.2 列出分支
select ref_name, commit_id, updated_at
from doc_ref
where doc_id = :doc_id and ref_type='branch'
order by ref_name;

-- 查看 commit 历史（first-parent 线性历史） （像 Git 的 --first-parent）
with recursive chain as (
    select c.id, c.created_at, 0 as depth
    from doc_ref r
             join doc_commit c on c.id = r.commit_id
    where r.doc_id=:doc_id and r.ref_name=:ref_name

    union all

    select p.parent_commit_id, c2.created_at, depth + 1
    from chain ch
             join doc_commit_parent p
                  on p.doc_id=:doc_id and p.child_commit_id = ch.id and p.parent_order=0
             join doc_commit c2 on c2.id = p.parent_commit_id
    where depth < :max_depth
)
select c.*
from chain ch
         join doc_commit c on c.id = ch.id
order by ch.depth;

-- 查两 commit 的最近公共祖先（LCA）
-- 这块 SQL 能写，但较长且性能要小心。工程上更常见做法：
-- 后端用图算法（BFS/双向 BFS），把 commit_parent 当边表查询即可；
-- 或加缓存表（可选：维护祖先闭包/高度）。
-- 我建议你先：不在数据库里硬写 LCA，在服务层做，查询 parent 边即可（PG 做存储+索引）。

-- 10) 你可能立刻想加的增强（建议）
-- 软删除/回收站：node 上 is_deleted 已有
-- 搜索：用 ES
-- 权限：space/doc 级 ACL 表（user_id, role）
-- 内容去重：content_hash + 复用 blob（如果你以后走对象存储更明显）