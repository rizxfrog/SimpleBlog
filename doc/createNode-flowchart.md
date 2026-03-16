# createNode 流程图

```mermaid
flowchart TD
    Start([开始]) --> CheckInput{input == null?}
    CheckInput -->|是 | ThrowInput[抛出异常:<br/>Input is required not null]
    CheckInput -->|否 | GetSpaceId[获取 spaceId]
    
    GetSpaceId --> CheckSpace{spaceId 存在?}
    CheckSpace -->|否 | ThrowSpace[抛出异常:<br/>Space not found]
    CheckSpace -->|是 | GetNodeType[获取 nodeType<br/>默认: DOC]
    
    GetNodeType --> NormalizeTitle[标准化 title<br/>strictUniqueNormalizeName]
    NormalizeTitle --> GetParentId[获取 parentId]
    
    GetParentId --> CheckParentId{parentId != null?}
    CheckParentId -->|是 | GetParent[获取 parent 节点]
    GetParent --> CheckParentSpace{parent.spaceId == spaceId?}
    CheckParentSpace -->|否 | ThrowParentSpace[抛出异常:<br/>Parent node does not<br/>belong to this space]
    CheckParentSpace -->|是 | CheckParentType{parent.nodeType == FOLDER?}
    CheckParentType -->|否 | ThrowParentType[抛出异常:<br/>Parent node must be a folder]
    CheckParentType -->|是 | CheckParentDeleted{parent.deleted == true?}
    CheckParentDeleted -->|是 | ThrowParentDeleted[抛出异常:<br/>Parent node is deleted]
    CheckParentDeleted -->|否 | GetSortKey
    
    CheckParentId -->|否 | GetSortKey[获取 sortKey<br/>从 Redis 服务获取]
    
    GetSortKey --> CreateNode[创建 DocNode 对象<br/>设置: spaceId, parentId,<br/>nodeType, title, sortKey, deleted]
    CreateNode --> InsertNode[插入节点到数据库<br/>docNodeMapper.insert]
    
    InsertNode --> CheckNodeType{nodeType == DOC?}
    CheckNodeType -->|否 | ReturnNode
    CheckNodeType -->|是 | CreateRepo[创建 DocRepo 对象<br/>设置: nodeId, defaultBranch, aclMode]
    
    CreateRepo --> InsertRepo[插入 repo 到数据库<br/>docRepoMapper.insert]
    InsertRepo --> GetDocId[获取 docId = repo.id]
    
    GetDocId --> InsertCommit[插入初始提交<br/>insertCommit:<br/>docId, authorId, title,<br/>content, message, salt]
    InsertCommit --> InsertBranch[插入分支引用<br/>docRefMapper.insertRef<br/>MAIN_BRANCH]
    
    InsertBranch --> InsertHead[插入 HEAD 引用<br/>docHeadMapper.insertHead]
    InsertHead --> ReturnNode[返回节点]
    
    ThrowInput --> End([结束])
    ThrowSpace --> End
    ThrowParentSpace --> End
    ThrowParentType --> End
    ThrowParentDeleted --> End
    ReturnNode --> End

    style Start fill:#90EE90
    style End fill:#FFB6C1
    style ThrowInput fill:#FF6B6B
    style ThrowSpace fill:#FF6B6B
    style ThrowParentSpace fill:#FF6B6B
    style ThrowParentType fill:#FF6B6B
    style ThrowParentDeleted fill:#FF6B6B
    style CheckInput fill:#FFE4B5
    style CheckSpace fill:#FFE4B5
    style CheckParentId fill:#FFE4B5
    style CheckParentSpace fill:#FFE4B5
    style CheckParentType fill:#FFE4B5
    style CheckParentDeleted fill:#FFE4B5
    style CheckNodeType fill:#FFE4B5
```

## 方法说明

`createNode` 方法用于在文档系统中创建新节点，主要流程如下：

1. **参数校验**：检查输入对象是否为 null
2. **空间验证**：验证 spaceId 是否存在
3. **节点类型**：获取节点类型（默认为 DOC）
4. **标题处理**：标准化并唯一化标题
5. **父节点验证**（如果有 parentId）：
   - 验证父节点属于同一空间
   - 验证父节点是文件夹类型
   - 验证父节点未被删除
6. **排序键**：从 Redis 服务获取排序键
7. **创建节点**：创建并插入 DocNode 记录
8. **如果是文档类型**：
   - 创建 DocRepo 记录
   - 插入初始提交（init commit）
   - 创建 MAIN_BRANCH 分支引用
   - 创建 HEAD 引用
