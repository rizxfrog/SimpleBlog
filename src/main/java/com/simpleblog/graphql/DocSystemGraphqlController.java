package com.simpleblog.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleblog.common.utils.RedisKeyQuickBuilder;
import com.simpleblog.common.utils.RedisUtils;
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
import com.simpleblog.model.entity.User;
import com.simpleblog.redisService.IDocSystemRedisService;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.IDocSystemService;
import com.simpleblog.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@AllArgsConstructor
public class DocSystemGraphqlController {
    private static final long DOC_TREE_CACHE_TTL_SECONDS = 60;

    private final IDocSystemService docSystemService;
    private final UserService userService;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;
    private final IDocSystemRedisService docSystemRedisService;

/*    public DocSystemGraphqlController(DocSystemService docSystemService,
                                      UserService userService,
                                      RedisUtils redisUtils,
                                      ObjectMapper objectMapper) {
        this.docSystemService = docSystemService;
        this.userService = userService;
        this.redisUtils = redisUtils;
        this.objectMapper = objectMapper;
    }*/

    @QueryMapping
    public List<DocSpace> docSpaces() {
        return docSystemService.listSpaces();
    }

    @QueryMapping
    public DocSpace docSpace(@Argument Long id) {
        return docSystemService.findSpace(id);
    }

    @QueryMapping
    public List<DocNode> docTree(@Argument Long spaceId,
                                 @Argument Boolean includeDeleted) {
        boolean include = Boolean.TRUE.equals(includeDeleted);
        String key = RedisKeyQuickBuilder.docTree(spaceId, include);
        List<DocNode> cached = readDocTreeCache(key);
        if (cached != null) {
            return cached;
        }
        List<DocNode> result = docSystemService.listTree(spaceId, include);
        writeDocTreeCache(key, result);
        return result;
    }

    @QueryMapping
    public DocNode docNode(@Argument Long id) {
        return docSystemService.findNode(id);
    }

    @QueryMapping
    public DocRepo docRepo(@Argument Long nodeId) {
        return docSystemService.findRepoByNodeId(nodeId);
    }

    @QueryMapping
    public List<DocRef> docRefs(@Argument Long docId) {
        return docSystemService.listRefs(docId);
    }

    @QueryMapping
    public DocCommit docLatestCommit(@Argument Long docId,
                                     @Argument String refName) {
        return docSystemService.findLatestCommit(docId, refName);
    }

    @QueryMapping
    public List<DocCommit> docCommitHistory(@Argument Long docId,
                                            @Argument String refName,
                                            @Argument Integer maxDepth) {
        int depth = maxDepth == null ? 50 : maxDepth;
        return docSystemService.listCommitHistory(docId, refName, depth);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocSpace createDocSpace(@Argument String name) {
        return docSystemService.createSpace(name);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocSpace renameDocSpace(@Argument Long id,
                                   @Argument String name) {
        DocSpace result = docSystemService.renameSpace(id, name);
        docSystemRedisService.invalidateDocTreeCacheBySpaceId(id);
        return result;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocSpace(@Argument Long id) {
        boolean deleted = docSystemService.deleteSpace(id);
        docSystemRedisService.invalidateDocTreeCacheBySpaceId(id);
        return deleted;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode createDocNode(@Argument DocNodeCreateInput input) {
        DocNode result = docSystemService.createNode(input, currentUserId());
        if (input != null) {
            docSystemRedisService.invalidateDocTreeCacheBySpaceId(input.spaceId());
        }
        return result;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode updateDocNode(@Argument Long id,
                                 @Argument DocNodeUpdateInput input) {
        Long spaceId = findSpaceIdByNodeId(id);
        DocNode result = docSystemService.updateNode(id, input);
        docSystemRedisService.invalidateDocTreeCacheBySpaceId(spaceId);
        return result;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode moveDocNode(@Argument Long id,
                               @Argument DocNodeMoveInput input) {
        Long spaceId = findSpaceIdByNodeId(id);
        DocNode result = docSystemService.moveNode(id, input);
        docSystemRedisService.invalidateDocTreeCacheBySpaceId(spaceId);
        return result;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocNode(@Argument Long id) {
        Long spaceId = findSpaceIdByNodeId(id);
        boolean deleted = docSystemService.deleteNode(id);
        docSystemRedisService.invalidateDocTreeCacheBySpaceId(spaceId);
        return deleted;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocRef createDocRef(@Argument Long docId,
                               @Argument String refName,
                               @Argument Long fromCommitId) {
        return docSystemService.createRef(docId, refName, fromCommitId);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocRef(@Argument Long docId,
                                @Argument String refName) {
        return docSystemService.deleteRef(docId, refName);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocCommit commitDoc(@Argument DocCommitInput input) {
        return docSystemService.commitDoc(input, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocCommit mergeDoc(@Argument DocMergeInput input) {
        return docSystemService.mergeDoc(input, currentUserId());
    }

    private Long currentUserId() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .map(User::getId)
                .orElse(null);
    }

    private Long findSpaceIdByNodeId(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        DocNode node = docSystemService.findNode(nodeId);
        return node == null ? null : node.getSpaceId();
    }

    private List<DocNode> readDocTreeCache(String key) {
        String value = redisUtils.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<DocNode>>() {});
        } catch (Exception ex) {
            redisUtils.delete(key);
            return null;
        }
    }

    private void writeDocTreeCache(String key, List<DocNode> docNodes) {
        try {
            redisUtils.setEx(
                    key,
                    objectMapper.writeValueAsString(docNodes),
                    DOC_TREE_CACHE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }
    }

/*    private void invalidateDocTreeCacheBySpaceId(Long spaceId) {
        if (spaceId == null) {
            return;
        }
        Set<String> keys = redisUtils.keys(RedisKeyBuilder.docTreePattern(spaceId));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisUtils.delete(keys);
    }*/
}
