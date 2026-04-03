package com.simpleblog.service;

import com.simpleblog.model.dto.DocumentCreateInput;
import com.simpleblog.model.dto.DocumentMoveInput;
import com.simpleblog.model.dto.DocumentSearchPage;
import com.simpleblog.model.dto.DocumentUpdateInput;
import com.simpleblog.model.entity.Document;

import java.util.List;

public interface DocumentService {
    String DEFAULT_PROJECT = "default";
    String DEFAULT_VERSION = "default";

    /**
     * 根据ID查找文档
     * @param id 文档ID
     * @param includeHidden 是否包含隐藏文档
     * @param project 项目名称
     * @param version 版本名称
     * @return 文档对象
     */
    Document findById(Long id, boolean includeHidden, String project, String version);

    /**
     * 查询文档列表
     * @param includeHidden 是否包含隐藏文档
     * @param project 项目名称
     * @param version 版本名称
     * @return 文档列表
     */
    List<Document> listDocuments(boolean includeHidden, String project, String version);

    /**
     * 查询文档树
     * @param rootId 根节点ID
     * @param includeHidden 是否包含隐藏文档
     * @param project 项目名称
     * @param version 版本名称
     * @return 文档树
     */
    List<Document> documentTree(Long rootId, boolean includeHidden, String project, String version);

    /**
     * 搜索文档
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @param includeHidden 是否包含隐藏文档
     * @param project 项目名称
     * @param version 版本名称
     * @return 文档分页结果
     */
    DocumentSearchPage searchDocuments(String query, int page, int size, boolean includeHidden, String project, String version);

    /**
     * 查询所有项目名称列表
     * @return 项目名称列表
     */
    List<String> listProjects();

    /**
     * 查询指定项目的所有版本列表
     * @param project 项目名称
     * @return 版本列表
     */
    List<String> listVersions(String project);

    /**
     * 创建项目(复制项目内容)
     * @param sourceProject 源项目
     * @param targetProject 目标项目
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean createProject(String sourceProject, String targetProject, Long userId);

    /**
     * 删除项目
     * @param project 项目名称
     * @return 是否成功
     */
    boolean deleteProject(String project);

    /**
     * 创建版本(复制版本内容)
     * @param project 项目名称
     * @param sourceVersion 源版本
     * @param targetVersion 目标版本
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean createVersion(String project, String sourceVersion, String targetVersion, Long userId);

    /**
     * 删除版本
     * @param project 项目名称
     * @param version 版本名称
     * @return 是否成功
     */
    boolean deleteVersion(String project, String version);

    /**
     * 创建文档
     * @param input 文档输入
     * @param userId 用户ID
     * @return 创建的文档
     */
    Document create(DocumentCreateInput input, Long userId);

    /**
     * 更新文档
     * @param id 文档ID
     * @param input 文档输入
     * @param userId 用户ID
     * @return 更新后的文档
     */
    Document update(Long id, DocumentUpdateInput input, Long userId);

    /**
     * 移动文档
     * @param id 文档ID
     * @param input 移动输入
     * @param userId 用户ID
     * @return 移动后的文档
     */
    Document move(Long id, DocumentMoveInput input, Long userId);

    /**
     * 设置文档隐藏状态
     * @param id 文档ID
     * @param hidden 是否隐藏
     * @param userId 用户ID
     * @return 更新后的文档
     */
    Document setHidden(Long id, boolean hidden, Long userId);

    /**
     * 删除文档
     * @param id 文档ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean delete(Long id, Long userId);
}
