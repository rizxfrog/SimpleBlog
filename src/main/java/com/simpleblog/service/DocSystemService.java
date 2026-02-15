package com.simpleblog.service;

import com.simpleblog.model.dto.DocCommitInput;
import com.simpleblog.model.dto.DocMergeInput;
import com.simpleblog.model.dto.DocNodeCreateInput;
import com.simpleblog.model.dto.DocNodeMoveInput;
import com.simpleblog.model.dto.DocNodeUpdateInput;
import com.simpleblog.model.entity.DocCommit;
import com.simpleblog.model.entity.DocNode;
import com.simpleblog.model.entity.DocRef;
import com.simpleblog.model.entity.DocRefType;
import com.simpleblog.model.entity.DocRepo;
import com.simpleblog.model.entity.DocSpace;
import com.simpleblog.model.entity.DocumentNodeType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocSystemService {
    public static final String MAIN_BRANCH = "refs/heads/main";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocSystemService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DocSpace> listSpaces() {
        return jdbcTemplate.query("""
                select id, name, create_at, update_at, delete_at
                from doc_space
                order by name asc, id asc
                """, DOC_SPACE_ROW_MAPPER);
    }

    public DocSpace findSpace(Long id) {
        if (id == null) {
            return null;
        }
        return queryOne("""
                select id, name, create_at, update_at, delete_at
                from doc_space
                where id = :id
                """, params("id", id), DOC_SPACE_ROW_MAPPER);
    }

    @Transactional
    public DocSpace createSpace(String name) {
        String nextName = normalizeName(name, "Untitled Space");
        ensureUniqueSpaceName(nextName, null);
        Long id = jdbcTemplate.queryForObject("""
                insert into doc_space(name)
                values (:name)
                returning id
                """, params("name", nextName), Long.class);
        return findSpace(id);
    }

    @Transactional
    public DocSpace renameSpace(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("Space id is required.");
        }
        DocSpace existing = findSpace(id);
        if (existing == null) {
            throw new IllegalArgumentException("Space not found: " + id);
        }
        String nextName = normalizeName(name, existing.getName());
        ensureUniqueSpaceName(nextName, id);
        jdbcTemplate.update("""
                update doc_space
                set name = :name
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", nextName));
        return findSpace(id);
    }

    @Transactional
    public boolean deleteSpace(Long id) {
        if (id == null) {
            return false;
        }
        return jdbcTemplate.update("""
                delete from doc_space
                where id = :id
                """, params("id", id)) > 0;
    }

    public List<DocNode> listTree(Long spaceId, boolean includeDeleted) {
        requireSpaceExists(spaceId);
        return jdbcTemplate.query("""
                select n.id,
                       n.space_id,
                       n.parent_id,
                       n.node_type::text as node_type,
                       n.title,
                       n.sort_key,
                       n.is_deleted as deleted,
                       n.create_at,
                       n.update_at,
                       d.id as doc_id
                from doc_node n
                left join doc d on d.node_id = n.id
                where n.space_id = :spaceId
                  and (:includeDeleted = true or n.is_deleted = false)
                order by n.parent_id nulls first, n.sort_key asc, n.id asc
                """, new MapSqlParameterSource()
                .addValue("spaceId", spaceId)
                .addValue("includeDeleted", includeDeleted), DOC_NODE_ROW_MAPPER);
    }

    public DocNode findNode(Long id) {
        if (id == null) {
            return null;
        }
        return queryOne("""
                select n.id,
                       n.space_id,
                       n.parent_id,
                       n.node_type::text as node_type,
                       n.title,
                       n.sort_key,
                       n.is_deleted as deleted,
                       n.create_at,
                       n.update_at,
                       d.id as doc_id
                from doc_node n
                left join doc d on d.node_id = n.id
                where n.id = :id
                """, params("id", id), DOC_NODE_ROW_MAPPER);
    }

    public DocRepo findRepoByNodeId(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        return queryOne("""
                select id, node_id, default_branch, acl_mode, create_at, update_at
                from doc
                where node_id = :nodeId
                """, params("nodeId", nodeId), DOC_REPO_ROW_MAPPER);
    }

    public List<DocRef> listRefs(Long docId) {
        requireDocExists(docId);
        return jdbcTemplate.query("""
                select doc_id, ref_name, commit_id, ref_type::text as ref_type, update_at
                from doc_ref
                where doc_id = :docId
                order by ref_type asc, ref_name asc
                """, params("docId", docId), DOC_REF_ROW_MAPPER);
    }

    public DocCommit findLatestCommit(Long docId, String refName) {
        requireDocExists(docId);
        String resolvedRef = normalizeRefName(refName);
        return queryOne("""
                select c.id,
                       c.doc_id,
                       encode(c.commit_hash, 'hex') as commit_hash,
                       c.author_id,
                       c.message,
                       c.create_at,
                       c.title,
                       c.content_md,
                       encode(c.content_hash, 'hex') as content_hash
                from doc_ref r
                join doc_commit c
                  on c.doc_id = r.doc_id
                 and c.id = r.commit_id
                where r.doc_id = :docId
                  and r.ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", resolvedRef), DOC_COMMIT_ROW_MAPPER);
    }

    public List<DocCommit> listCommitHistory(Long docId, String refName, int maxDepth) {
        requireDocExists(docId);
        String resolvedRef = normalizeRefName(refName);
        int depth = maxDepth <= 0 ? 50 : Math.min(maxDepth, 500);
        return jdbcTemplate.query("""
                with recursive chain as (
                    select c.id, c.create_at, 0 as depth
                    from doc_ref r
                    join doc_commit c
                      on c.doc_id = r.doc_id
                     and c.id = r.commit_id
                    where r.doc_id = :docId
                      and r.ref_name = :refName
                    union all
                    select p.parent_commit_id, c2.create_at, ch.depth + 1
                    from chain ch
                    join doc_commit_parent p
                      on p.doc_id = :docId
                     and p.child_commit_id = ch.id
                     and p.parent_order = 0
                    join doc_commit c2
                      on c2.doc_id = p.doc_id
                     and c2.id = p.parent_commit_id
                    where ch.depth < :depth
                )
                select c.id,
                       c.doc_id,
                       encode(c.commit_hash, 'hex') as commit_hash,
                       c.author_id,
                       c.message,
                       c.create_at,
                       c.title,
                       c.content_md,
                       encode(c.content_hash, 'hex') as content_hash,
                       ch.depth
                from chain ch
                join doc_commit c
                  on c.doc_id = :docId
                 and c.id = ch.id
                order by ch.depth asc
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", resolvedRef)
                .addValue("depth", depth), DOC_COMMIT_ROW_MAPPER);
    }

    @Transactional
    public DocNode createNode(DocNodeCreateInput input, Long authorId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        Long spaceId = input.spaceId();
        requireSpaceExists(spaceId);

        DocumentNodeType nodeType = input.nodeType() == null ? DocumentNodeType.DOC : input.nodeType();
        String title = normalizeName(input.title(), "Untitled");
        Long parentId = input.parentId();
        if (parentId != null) {
            DocNode parent = requireNode(parentId);
            if (!Objects.equals(parent.getSpaceId(), spaceId)) {
                throw new IllegalArgumentException("Parent node does not belong to this space.");
            }
            if (parent.getNodeType() != DocumentNodeType.FOLDER) {
                throw new IllegalArgumentException("Parent node must be a folder.");
            }
            if (Boolean.TRUE.equals(parent.getDeleted())) {
                throw new IllegalArgumentException("Parent node is deleted.");
            }
        }
        int sortKey = input.sortKey() != null ? input.sortKey() : nextSortKey(spaceId, parentId);

        Long nodeId = jdbcTemplate.queryForObject("""
                insert into doc_node(space_id, parent_id, node_type, title, sort_key, is_deleted)
                values (:spaceId, :parentId, cast(:nodeType as doc_node_type), :title, :sortKey, false)
                returning id
                """, new MapSqlParameterSource()
                .addValue("spaceId", spaceId)
                .addValue("parentId", parentId)
                .addValue("nodeType", nodeType.getValue())
                .addValue("title", title)
                .addValue("sortKey", sortKey), Long.class);

        if (nodeType == DocumentNodeType.DOC) {
            Long docId = jdbcTemplate.queryForObject("""
                    insert into doc(node_id, default_branch)
                    values (:nodeId, :defaultBranch)
                    returning id
                    """, new MapSqlParameterSource()
                    .addValue("nodeId", nodeId)
                    .addValue("defaultBranch", MAIN_BRANCH), Long.class);
            DocCommit initCommit = insertCommit(docId, authorId, title, safeContent(input.contentMd()), "Initial commit", "init-" + UUID.randomUUID());
            jdbcTemplate.update("""
                    insert into doc_ref(doc_id, ref_name, commit_id, ref_type)
                    values (:docId, :refName, :commitId, cast(:refType as doc_ref_type))
                    """, new MapSqlParameterSource()
                    .addValue("docId", docId)
                    .addValue("refName", MAIN_BRANCH)
                    .addValue("commitId", initCommit.getId())
                    .addValue("refType", DocRefType.BRANCH.getValue()));
            jdbcTemplate.update("""
                    insert into doc_head(doc_id, head_ref, ref_version)
                    values (:docId, :headRef, 0)
                    """, new MapSqlParameterSource()
                    .addValue("docId", docId)
                    .addValue("headRef", MAIN_BRANCH));
        }
        return requireNode(nodeId);
    }

    @Transactional
    public DocNode updateNode(Long id, DocNodeUpdateInput input) {
        DocNode existing = requireNode(id);
        if (input == null) {
            return existing;
        }
        String title = input.title() == null ? null : normalizeName(input.title(), existing.getTitle());
        jdbcTemplate.update("""
                update doc_node
                set title = coalesce(:title, title),
                    sort_key = coalesce(:sortKey, sort_key),
                    is_deleted = coalesce(:deleted, is_deleted)
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", title)
                .addValue("sortKey", input.sortKey())
                .addValue("deleted", input.deleted()));
        return requireNode(id);
    }

    @Transactional
    public DocNode moveNode(Long id, DocNodeMoveInput input) {
        DocNode node = requireNode(id);
        if (input == null) {
            return node;
        }
        Long targetParentId = input.parentId();
        if (targetParentId != null) {
            DocNode parent = requireNode(targetParentId);
            if (!Objects.equals(parent.getSpaceId(), node.getSpaceId())) {
                throw new IllegalArgumentException("Target parent must be in same space.");
            }
            if (parent.getNodeType() != DocumentNodeType.FOLDER) {
                throw new IllegalArgumentException("Target parent must be a folder.");
            }
            if (containsNode(id, targetParentId)) {
                throw new IllegalArgumentException("Cannot move node under its descendant.");
            }
        }
        int sortKey = input.sortKey() != null ? input.sortKey() : nextSortKey(node.getSpaceId(), targetParentId);
        jdbcTemplate.update("""
                update doc_node
                set parent_id = :parentId,
                    sort_key = :sortKey
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("parentId", targetParentId)
                .addValue("sortKey", sortKey));
        return requireNode(id);
    }

    @Transactional
    public boolean deleteNode(Long id) {
        requireNode(id);
        return jdbcTemplate.update("""
                with recursive subtree as (
                    select id
                    from doc_node
                    where id = :id
                    union all
                    select n.id
                    from doc_node n
                    join subtree s on n.parent_id = s.id
                )
                update doc_node
                set is_deleted = true
                where id in (select id from subtree)
                """, params("id", id)) > 0;
    }

    @Transactional
    public DocRef createRef(Long docId, String refName, Long fromCommitId) {
        requireDocExists(docId);
        if (fromCommitId == null) {
            throw new IllegalArgumentException("fromCommitId is required.");
        }
        String normalizedRef = normalizeRefName(refName);
        DocRefType refType = parseRefType(normalizedRef);
        long commitExists = jdbcTemplate.queryForObject("""
                select count(1)
                from doc_commit
                where doc_id = :docId
                  and id = :commitId
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("commitId", fromCommitId), Long.class);
        if (commitExists == 0) {
            throw new IllegalArgumentException("Commit not found in this doc.");
        }
        jdbcTemplate.update("""
                insert into doc_ref(doc_id, ref_name, commit_id, ref_type)
                values (:docId, :refName, :commitId, cast(:refType as doc_ref_type))
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", normalizedRef)
                .addValue("commitId", fromCommitId)
                .addValue("refType", refType.getValue()));
        ensureHeadExists(docId, normalizedRef, refType);
        return requireRef(docId, normalizedRef);
    }

    @Transactional
    public boolean deleteRef(Long docId, String refName) {
        requireDocExists(docId);
        String normalizedRef = normalizeRefName(refName);
        if (MAIN_BRANCH.equals(normalizedRef)) {
            throw new IllegalArgumentException("Main branch cannot be deleted.");
        }
        String currentHead = queryOne("""
                select head_ref
                from doc_head
                where doc_id = :docId
                """, params("docId", docId), (rs, rowNum) -> rs.getString("head_ref"));
        if (Objects.equals(currentHead, normalizedRef)) {
            throw new IllegalArgumentException("Cannot delete current HEAD ref.");
        }
        return jdbcTemplate.update("""
                delete from doc_ref
                where doc_id = :docId
                  and ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", normalizedRef)) > 0;
    }

    @Transactional
    public DocCommit commitDoc(DocCommitInput input, Long authorId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        Long docId = input.docId();
        requireDocExists(docId);
        String refName = normalizeRefName(input.refName());
        Long currentTip = lockRefTip(docId, refName);
        if (currentTip == null) {
            throw new IllegalArgumentException("Ref not found: " + refName);
        }
        if (input.baseCommitId() != null && !Objects.equals(input.baseCommitId(), currentTip)) {
            throw new IllegalStateException("non-fast-forward: base commit is stale");
        }

        String title = normalizeName(input.title(), "Untitled");
        String content = safeContent(input.contentMd());
        String message = normalizeName(input.message(), "Update document");
        DocCommit commit = insertCommit(docId, authorId, title, content, message,
                "commit-" + docId + "-" + currentTip + "-" + System.nanoTime());

        jdbcTemplate.update("""
                insert into doc_commit_parent(doc_id, child_commit_id, parent_commit_id, parent_order)
                values (:docId, :childCommitId, :parentCommitId, 0)
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("childCommitId", commit.getId())
                .addValue("parentCommitId", currentTip));
        jdbcTemplate.update("""
                update doc_ref
                set commit_id = :commitId
                where doc_id = :docId
                  and ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("commitId", commit.getId())
                .addValue("docId", docId)
                .addValue("refName", refName));
        syncNodeTitle(docId, title);
        return commit;
    }

    @Transactional
    public DocCommit mergeDoc(DocMergeInput input, Long authorId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        Long docId = input.docId();
        requireDocExists(docId);
        String targetRef = normalizeRefName(input.targetRef());
        String sourceRef = normalizeRefName(input.sourceRef());
        Long targetTip = lockRefTip(docId, targetRef);
        if (targetTip == null) {
            throw new IllegalArgumentException("Target ref not found: " + targetRef);
        }
        if (input.targetBaseCommitId() != null && !Objects.equals(input.targetBaseCommitId(), targetTip)) {
            throw new IllegalStateException("non-fast-forward: target branch is stale");
        }
        Long sourceTip = findRefTip(docId, sourceRef);
        if (sourceTip == null) {
            throw new IllegalArgumentException("Source ref not found: " + sourceRef);
        }
        String title = normalizeName(input.title(), "Merge document");
        String content = safeContent(input.contentMd());
        String message = normalizeName(input.message(), "Merge " + sourceRef + " into " + targetRef);
        DocCommit commit = insertCommit(docId, authorId, title, content, message,
                "merge-" + docId + "-" + targetTip + "-" + sourceTip + "-" + System.nanoTime());

        jdbcTemplate.update("""
                insert into doc_commit_parent(doc_id, child_commit_id, parent_commit_id, parent_order)
                values (:docId, :childCommitId, :parentCommitId, 0)
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("childCommitId", commit.getId())
                .addValue("parentCommitId", targetTip));
        if (!Objects.equals(sourceTip, targetTip)) {
            jdbcTemplate.update("""
                    insert into doc_commit_parent(doc_id, child_commit_id, parent_commit_id, parent_order)
                    values (:docId, :childCommitId, :parentCommitId, 1)
                    """, new MapSqlParameterSource()
                    .addValue("docId", docId)
                    .addValue("childCommitId", commit.getId())
                    .addValue("parentCommitId", sourceTip));
        }
        jdbcTemplate.update("""
                update doc_ref
                set commit_id = :commitId
                where doc_id = :docId
                  and ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("commitId", commit.getId())
                .addValue("docId", docId)
                .addValue("refName", targetRef));
        syncNodeTitle(docId, title);
        return commit;
    }

    private DocCommit insertCommit(Long docId,
                                   Long authorId,
                                   String title,
                                   String contentMd,
                                   String message,
                                   String salt) {
        byte[] contentHash = sha256(contentMd);
        String payload = docId + "|" + title + "|" + message + "|" + salt + "|" + Base64.getEncoder().encodeToString(contentHash);
        byte[] commitHash = sha256(payload);
        Long commitId = jdbcTemplate.queryForObject("""
                insert into doc_commit(doc_id, commit_hash, author_id, message, title, content_md)
                values (:docId, :commitHash, :authorId, :message, :title, :contentMd)
                returning id
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("commitHash", commitHash)
                .addValue("authorId", authorId)
                .addValue("message", message)
                .addValue("title", title)
                .addValue("contentMd", contentMd), Long.class);
        return requireCommit(docId, commitId);
    }

    private void syncNodeTitle(Long docId, String title) {
        jdbcTemplate.update("""
                update doc_node
                set title = :title
                where id = (select node_id from doc where id = :docId)
                """, new MapSqlParameterSource()
                .addValue("title", title)
                .addValue("docId", docId));
    }

    private int nextSortKey(Long spaceId, Long parentId) {
        Integer next = jdbcTemplate.queryForObject("""
                select coalesce(max(sort_key), -1) + 1
                from doc_node
                where space_id = :spaceId
                  and parent_id is not distinct from :parentId
                """, new MapSqlParameterSource()
                .addValue("spaceId", spaceId)
                .addValue("parentId", parentId), Integer.class);
        return next == null ? 0 : next;
    }

    private boolean containsNode(Long rootNodeId, Long candidateId) {
        Long count = jdbcTemplate.queryForObject("""
                with recursive subtree as (
                    select id
                    from doc_node
                    where id = :rootNodeId
                    union all
                    select n.id
                    from doc_node n
                    join subtree s on n.parent_id = s.id
                )
                select count(1)
                from subtree
                where id = :candidateId
                """, new MapSqlParameterSource()
                .addValue("rootNodeId", rootNodeId)
                .addValue("candidateId", candidateId), Long.class);
        return count != null && count > 0;
    }

    private Long findRefTip(Long docId, String refName) {
        return queryOne("""
                select commit_id
                from doc_ref
                where doc_id = :docId
                  and ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", refName), (rs, rowNum) -> rs.getLong("commit_id"));
    }

    private Long lockRefTip(Long docId, String refName) {
        return queryOne("""
                select commit_id
                from doc_ref
                where doc_id = :docId
                  and ref_name = :refName
                for update
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", refName), (rs, rowNum) -> rs.getLong("commit_id"));
    }

    private void ensureHeadExists(Long docId, String refName, DocRefType refType) {
        if (refType != DocRefType.BRANCH) {
            return;
        }
        Long count = jdbcTemplate.queryForObject("""
                select count(1)
                from doc_head
                where doc_id = :docId
                """, params("docId", docId), Long.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into doc_head(doc_id, head_ref, ref_version)
                values (:docId, :headRef, 0)
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("headRef", refName));
    }

    private DocCommit requireCommit(Long docId, Long commitId) {
        DocCommit commit = queryOne("""
                select id,
                       doc_id,
                       encode(commit_hash, 'hex') as commit_hash,
                       author_id,
                       message,
                       create_at,
                       title,
                       content_md,
                       encode(content_hash, 'hex') as content_hash
                from doc_commit
                where doc_id = :docId
                  and id = :id
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("id", commitId), DOC_COMMIT_ROW_MAPPER);
        if (commit == null) {
            throw new IllegalArgumentException("Commit not found: " + commitId);
        }
        return commit;
    }

    private DocRef requireRef(Long docId, String refName) {
        DocRef ref = queryOne("""
                select doc_id, ref_name, commit_id, ref_type::text as ref_type, update_at
                from doc_ref
                where doc_id = :docId
                  and ref_name = :refName
                """, new MapSqlParameterSource()
                .addValue("docId", docId)
                .addValue("refName", refName), DOC_REF_ROW_MAPPER);
        if (ref == null) {
            throw new IllegalArgumentException("Ref not found: " + refName);
        }
        return ref;
    }

    private DocNode requireNode(Long nodeId) {
        DocNode node = findNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        return node;
    }

    private void requireDocExists(Long docId) {
        if (docId == null) {
            throw new IllegalArgumentException("docId is required.");
        }
        Long count = jdbcTemplate.queryForObject("""
                select count(1)
                from doc
                where id = :id
                """, params("id", docId), Long.class);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Doc not found: " + docId);
        }
    }

    private void requireSpaceExists(Long spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("spaceId is required.");
        }
        Long count = jdbcTemplate.queryForObject("""
                select count(1)
                from doc_space
                where id = :id
                """, params("id", spaceId), Long.class);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Space not found: " + spaceId);
        }
    }

    private void ensureUniqueSpaceName(String name, Long excludingId) {
        Long count = jdbcTemplate.queryForObject("""
                select count(1)
                from doc_space
                where lower(name) = lower(:name)
                  and (:excludingId is null or id <> :excludingId)
                """, new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("excludingId", excludingId), Long.class);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Space already exists: " + name);
        }
    }

    private String normalizeRefName(String refName) {
        String value = refName == null || refName.isBlank() ? MAIN_BRANCH : refName.trim();
        parseRefType(value);
        return value;
    }

    private DocRefType parseRefType(String refName) {
        if (refName.startsWith("refs/heads/")) {
            return DocRefType.BRANCH;
        }
        if (refName.startsWith("refs/tags/")) {
            return DocRefType.TAG;
        }
        throw new IllegalArgumentException("Invalid ref name, expected refs/heads/* or refs/tags/*");
    }

    private String normalizeName(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }

    private String safeContent(String contentMd) {
        return contentMd == null ? "" : contentMd;
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private MapSqlParameterSource params(String key, Object value) {
        return new MapSqlParameterSource().addValue(key, value);
    }

    private <T> T queryOne(String sql, MapSqlParameterSource params, RowMapper<T> mapper) {
        List<T> rows = jdbcTemplate.query(sql, params, mapper);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static final RowMapper<DocSpace> DOC_SPACE_ROW_MAPPER = (rs, rowNum) -> {
        DocSpace space = new DocSpace();
        space.setId(rs.getLong("id"));
        space.setName(rs.getString("name"));
        space.setCreateAt(toLocalDateTime(rs.getTimestamp("create_at")));
        space.setUpdateAt(toLocalDateTime(rs.getTimestamp("update_at")));
        space.setDeleteAt(toLocalDateTime(rs.getTimestamp("delete_at")));
        return space;
    };

    private static final RowMapper<DocNode> DOC_NODE_ROW_MAPPER = (rs, rowNum) -> {
        DocNode node = new DocNode();
        node.setId(rs.getLong("id"));
        node.setSpaceId(rs.getLong("space_id"));
        long parent = rs.getLong("parent_id");
        node.setParentId(rs.wasNull() ? null : parent);
        node.setNodeType(DocumentNodeType.fromValue(rs.getString("node_type")));
        node.setTitle(rs.getString("title"));
        node.setSortKey(rs.getInt("sort_key"));
        node.setDeleted(rs.getBoolean("deleted"));
        node.setCreateAt(toLocalDateTime(rs.getTimestamp("create_at")));
        node.setUpdateAt(toLocalDateTime(rs.getTimestamp("update_at")));
        long docId = rs.getLong("doc_id");
        node.setDocId(rs.wasNull() ? null : docId);
        return node;
    };

    private static final RowMapper<DocRepo> DOC_REPO_ROW_MAPPER = (rs, rowNum) -> {
        DocRepo repo = new DocRepo();
        repo.setId(rs.getLong("id"));
        repo.setNodeId(rs.getLong("node_id"));
        repo.setDefaultBranch(rs.getString("default_branch"));
        repo.setAclMode(rs.getString("acl_mode"));
        repo.setCreateAt(toLocalDateTime(rs.getTimestamp("create_at")));
        repo.setUpdateAt(toLocalDateTime(rs.getTimestamp("update_at")));
        return repo;
    };

    private static final RowMapper<DocRef> DOC_REF_ROW_MAPPER = (rs, rowNum) -> {
        DocRef ref = new DocRef();
        ref.setDocId(rs.getLong("doc_id"));
        ref.setRefName(rs.getString("ref_name"));
        ref.setCommitId(rs.getLong("commit_id"));
        ref.setRefType(DocRefType.fromValue(rs.getString("ref_type")));
        ref.setUpdateAt(toLocalDateTime(rs.getTimestamp("update_at")));
        return ref;
    };

    private static final RowMapper<DocCommit> DOC_COMMIT_ROW_MAPPER = (rs, rowNum) -> {
        DocCommit commit = new DocCommit();
        commit.setId(rs.getLong("id"));
        commit.setDocId(rs.getLong("doc_id"));
        commit.setCommitHash(rs.getString("commit_hash"));
        long authorId = rs.getLong("author_id");
        commit.setAuthorId(rs.wasNull() ? null : authorId);
        commit.setMessage(rs.getString("message"));
        commit.setCreateAt(toLocalDateTime(rs.getTimestamp("create_at")));
        commit.setTitle(rs.getString("title"));
        commit.setContentMd(rs.getString("content_md"));
        commit.setContentHash(rs.getString("content_hash"));
        try {
            int depth = rs.getInt("depth");
            commit.setDepth(rs.wasNull() ? null : depth);
        } catch (Exception ignore) {
            commit.setDepth(null);
        }
        return commit;
    };

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
