package com.simpleblog.service;

import com.simpleblog.model.dto.DocCommitInput;
import com.simpleblog.model.dto.DocMergeInput;
import com.simpleblog.model.dto.DocNodeCreateInput;
import com.simpleblog.model.dto.DocNodeMoveInput;
import com.simpleblog.model.dto.DocNodeUpdateInput;
import com.simpleblog.model.entity.DocCommit;
import com.simpleblog.model.entity.DocNode;
import com.simpleblog.model.entity.DocRef;
import com.simpleblog.model.entity.DocRepo;
import com.simpleblog.model.entity.DocSpace;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档系统核心服务接口
 * 定义了文档空间、节点、文档仓库、引用和提交的管理操作
 */
public interface IDocSystemService {

    // ========== Space 相关 ==========

    /**
     * 列出所有文档空间
     */
    List<DocSpace> listSpaces();

    /**
     * 根据ID查找文档空间
     */
    DocSpace findSpace(Long id);

    /**
     * 创建新的文档空间
     */
    @Transactional
    DocSpace createSpace(String name);

    /**
     * 重命名文档空间
     */
    @Transactional
    DocSpace renameSpace(Long id, String name);

    /**
     * 删除文档空间
     */
    @Transactional
    boolean deleteSpace(Long id);


    // ========== Node 相关 ==========

    /**
     * 列出空间下的所有文档节点树
     * @param includeDeleted 是否包含已删除的节点
     */
    List<DocNode> listTree(Long spaceId, boolean includeDeleted);

    /**
     * 根据ID查找文档节点
     */
    DocNode findNode(Long id);

    /**
     * 根据节点ID查找对应的文档仓库
     */
    DocRepo findRepoByNodeId(Long nodeId);

    /**
     * 创建新的文档节点
     * @param authorId 作者ID
     */
    @Transactional
    DocNode createNode(DocNodeCreateInput input, Long authorId);

    /**
     * 更新文档节点
     */
    @Transactional
    DocNode updateNode(Long id, DocNodeUpdateInput input);

    /**
     * 移动文档节点
     */
    @Transactional
    DocNode moveNode(Long id, DocNodeMoveInput input);

    /**
     * 删除文档节点（逻辑删除）
     */
    @Transactional
    boolean deleteNode(Long id);


    // ========== Ref 相关 ==========

    /**
     * 列出文档的所有引用（分支/标签）
     */
    List<DocRef> listRefs(Long docId);

    /**
     * 创建新的引用
     */
    @Transactional
    DocRef createRef(Long docId, String refName, Long fromCommitId);

    /**
     * 删除引用
     */
    @Transactional
    boolean deleteRef(Long docId, String refName);


    // ========== Commit 相关 ==========

    /**
     * 查找指定引用的最新提交
     */
    DocCommit findLatestCommit(Long docId, String refName);

    /**
     * 列出提交历史
     * @param maxDepth 最大深度，0表示使用默认值
     */
    List<DocCommit> listCommitHistory(Long docId, String refName, int maxDepth);

    /**
     * 提交文档更新
     * @param authorId 作者ID
     */
    @Transactional
    DocCommit commitDoc(DocCommitInput input, Long authorId);

    /**
     * 合并分支
     * @param authorId 作者ID
     */
    @Transactional
    DocCommit mergeDoc(DocMergeInput input, Long authorId);
}