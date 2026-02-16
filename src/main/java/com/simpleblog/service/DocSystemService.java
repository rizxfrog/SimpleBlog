package com.simpleblog.service;

import com.simpleblog.common.utils.StrictUniqueTimestamp;
import com.simpleblog.mapper.DocCommitMapper;
import com.simpleblog.mapper.DocCommitParentMapper;
import com.simpleblog.mapper.DocHeadMapper;
import com.simpleblog.mapper.DocNodeMapper;
import com.simpleblog.mapper.DocRefMapper;
import com.simpleblog.mapper.DocRepoMapper;
import com.simpleblog.mapper.DocSpaceMapper;
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
import com.simpleblog.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocSystemService {
    public static final String MAIN_BRANCH = "refs/heads/main";

    private final DocSpaceMapper docSpaceMapper;
    private final DocNodeMapper docNodeMapper;
    private final DocRepoMapper docRepoMapper;
    private final DocCommitMapper docCommitMapper;
    private final DocRefMapper docRefMapper;
    private final DocHeadMapper docHeadMapper;
    private final DocCommitParentMapper docCommitParentMapper;
    private final JwtService jwtService;

    public DocSystemService(DocSpaceMapper docSpaceMapper,
                            DocNodeMapper docNodeMapper,
                            DocRepoMapper docRepoMapper,
                            DocCommitMapper docCommitMapper,
                            DocRefMapper docRefMapper,
                            DocHeadMapper docHeadMapper,
                            DocCommitParentMapper docCommitParentMapper,
                            JwtService jwtService) {
        this.docSpaceMapper = docSpaceMapper;
        this.docNodeMapper = docNodeMapper;
        this.docRepoMapper = docRepoMapper;
        this.docCommitMapper = docCommitMapper;
        this.docRefMapper = docRefMapper;
        this.docHeadMapper = docHeadMapper;
        this.docCommitParentMapper = docCommitParentMapper;
        this.jwtService = jwtService;
    }

    public List<DocSpace> listSpaces() {
        return docSpaceMapper.listAll();
    }

    public DocSpace findSpace(Long id) {
        if (id == null) {
            return null;
        }
        return docSpaceMapper.selectById(id);
    }

    @Transactional
    public DocSpace createSpace(String name) {
        String nextName = strictUniqueNormalizeName(name, "Untitled Space");
//        ensureUniqueSpaceName(nextName, null); // 数据库里有 unique(name, owner_id) where not is_deleted;
        DocSpace space = new DocSpace();
        space.setName(nextName);
        Long userId = jwtService.currentUserId();
        space.setOwnerId(userId);
        docSpaceMapper.insert(space);
        return findSpace(space.getId());
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

        existing.setName(nextName);
        docSpaceMapper.updateById(existing);
        return findSpace(id);
    }

    @Transactional
    public boolean deleteSpace(Long id) {
        if (id == null) {
            return false;
        }
        return docSpaceMapper.deleteById(id) > 0;
    }

    public List<DocNode> listTree(Long spaceId, boolean includeDeleted) {
        requireSpaceExists(spaceId);
        return docNodeMapper.listBySpace(spaceId, includeDeleted);
    }

    public DocNode findNode(Long id) {
        if (id == null) {
            return null;
        }
        return docNodeMapper.selectByIdWithDoc(id);
    }

    public DocRepo findRepoByNodeId(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        return docRepoMapper.selectByNodeId(nodeId);
    }

    public List<DocRef> listRefs(Long docId) {
        requireDocExists(docId);
        return docRefMapper.listByDocId(docId);
    }

    public DocCommit findLatestCommit(Long docId, String refName) {
        requireDocExists(docId);
        String resolvedRef = normalizeRefName(refName);
        return docCommitMapper.findLatestByRef(docId, resolvedRef);
    }

    public List<DocCommit> listCommitHistory(Long docId, String refName, int maxDepth) {
        requireDocExists(docId);
        String resolvedRef = normalizeRefName(refName);
        int depth = maxDepth <= 0 ? 50 : Math.min(maxDepth, 500);
        return docCommitMapper.listHistory(docId, resolvedRef, depth);
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
        DocNode node = new DocNode();
        node.setSpaceId(spaceId);
        node.setParentId(parentId);
        node.setNodeType(nodeType);
        node.setTitle(title);
        node.setSortKey(sortKey);
        node.setDeleted(false);
        docNodeMapper.insert(node);

        if (nodeType == DocumentNodeType.DOC) {
            DocRepo repo = new DocRepo();
            repo.setNodeId(node.getId());
            repo.setDefaultBranch(MAIN_BRANCH);
            repo.setAclMode("inherit");
            docRepoMapper.insert(repo);

            Long docId = repo.getId();
            DocCommit initCommit = insertCommit(docId, authorId, title, safeContent(input.contentMd()), "Initial commit", "init-" + UUID.randomUUID());
            docRefMapper.insertRef(docId, MAIN_BRANCH, initCommit.getId(), DocRefType.BRANCH.getValue());
            docHeadMapper.insertHead(docId, MAIN_BRANCH);
        }
        return requireNode(node.getId());
    }

    @Transactional
    public DocNode updateNode(Long id, DocNodeUpdateInput input) {
        DocNode existing = requireNode(id);
        if (input == null) {
            return existing;
        }
        if (input.title() != null) {
            existing.setTitle(normalizeName(input.title(), existing.getTitle()));
        }
        if (input.sortKey() != null) {
            existing.setSortKey(input.sortKey());
        }
        if (input.deleted() != null) {
            existing.setDeleted(input.deleted());
        }
        docNodeMapper.updateById(existing);
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
        node.setParentId(targetParentId);
        node.setSortKey(sortKey);
        docNodeMapper.updateById(node);
        return requireNode(id);
    }

    @Transactional
    public boolean deleteNode(Long id) {
        requireNode(id);
        return docNodeMapper.markSubtreeDeleted(id) > 0;
    }

    @Transactional
    public DocRef createRef(Long docId, String refName, Long fromCommitId) {
        requireDocExists(docId);
        if (fromCommitId == null) {
            throw new IllegalArgumentException("fromCommitId is required.");
        }
        String normalizedRef = normalizeRefName(refName);
        DocRefType refType = parseRefType(normalizedRef);

        long commitExists = docCommitMapper.countByDocAndId(docId, fromCommitId);
        if (commitExists == 0) {
            throw new IllegalArgumentException("Commit not found in this doc.");
        }
        docRefMapper.insertRef(docId, normalizedRef, fromCommitId, refType.getValue());
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
        String currentHead = docHeadMapper.findHeadRef(docId);
        if (Objects.equals(currentHead, normalizedRef)) {
            throw new IllegalArgumentException("Cannot delete current HEAD ref.");
        }
        return docRefMapper.deleteRef(docId, normalizedRef) > 0;
    }

    @Transactional
    public DocCommit commitDoc(DocCommitInput input, Long authorId) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required.");
        }
        Long docId = input.docId();
        requireDocExists(docId);
        String refName = normalizeRefName(input.refName());
        Long currentTip = docRefMapper.lockRefTip(docId, refName);
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

        docCommitParentMapper.insertParent(docId, commit.getId(), currentTip, 0);
        docRefMapper.updateRefCommit(docId, refName, commit.getId());
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

        Long targetTip = docRefMapper.lockRefTip(docId, targetRef);
        if (targetTip == null) {
            throw new IllegalArgumentException("Target ref not found: " + targetRef);
        }
        if (input.targetBaseCommitId() != null && !Objects.equals(input.targetBaseCommitId(), targetTip)) {
            throw new IllegalStateException("non-fast-forward: target branch is stale");
        }
        Long sourceTip = docRefMapper.findRefTip(docId, sourceRef);
        if (sourceTip == null) {
            throw new IllegalArgumentException("Source ref not found: " + sourceRef);
        }

        String title = normalizeName(input.title(), "Merge document");
        String content = safeContent(input.contentMd());
        String message = normalizeName(input.message(), "Merge " + sourceRef + " into " + targetRef);
        DocCommit commit = insertCommit(docId, authorId, title, content, message,
                "merge-" + docId + "-" + targetTip + "-" + sourceTip + "-" + System.nanoTime());

        docCommitParentMapper.insertParent(docId, commit.getId(), targetTip, 0);
        if (!Objects.equals(sourceTip, targetTip)) {
            docCommitParentMapper.insertParent(docId, commit.getId(), sourceTip, 1);
        }
        docRefMapper.updateRefCommit(docId, targetRef, commit.getId());
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

        Long commitId = docCommitMapper.insertReturningId(docId, commitHash, authorId, message, title, contentMd);
        return requireCommit(docId, commitId);
    }

    private void syncNodeTitle(Long docId, String title) {
        Long nodeId = docRepoMapper.findNodeIdByDocId(docId);
        if (nodeId != null) {
            docNodeMapper.updateTitle(nodeId, title);
        }
    }

    private int nextSortKey(Long spaceId, Long parentId) {
        Integer next = docNodeMapper.nextSortKey(spaceId, parentId);
        return next == null ? 0 : next;
    }

    private boolean containsNode(Long rootNodeId, Long candidateId) {
        return docNodeMapper.countContains(rootNodeId, candidateId) > 0;
    }

    private void ensureHeadExists(Long docId, String refName, DocRefType refType) {
        if (refType != DocRefType.BRANCH) {
            return;
        }
        if (docHeadMapper.countByDocId(docId) == 0) {
            docHeadMapper.insertHead(docId, refName);
        }
    }

    private DocCommit requireCommit(Long docId, Long commitId) {
        DocCommit commit = docCommitMapper.selectByDocAndId(docId, commitId);
        if (commit == null) {
            throw new IllegalArgumentException("Commit not found: " + commitId);
        }
        return commit;
    }

    private DocRef requireRef(Long docId, String refName) {
        DocRef ref = docRefMapper.selectByDocAndName(docId, refName);
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
        if (docRepoMapper.countById(docId) == 0) {
            throw new IllegalArgumentException("Doc not found: " + docId);
        }
    }

    private void requireSpaceExists(Long spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("spaceId is required.");
        }
        if (docSpaceMapper.selectById(spaceId) == null) {
            throw new IllegalArgumentException("Space not found: " + spaceId);
        }
    }

    private void ensureUniqueSpaceName(String name, Long excludingId) {
        long count = docSpaceMapper.countByName(name, excludingId);
        if (count > 0) {
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

    private String strictUniqueNormalizeName(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback + " " + StrictUniqueTimestamp.next();
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
}
