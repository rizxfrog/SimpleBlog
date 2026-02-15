package com.simpleblog.graphql;

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
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.DocSystemService;
import com.simpleblog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DocSystemGraphqlController {
    private final DocSystemService docSystemService;
    private final UserService userService;

    public DocSystemGraphqlController(DocSystemService docSystemService,
                                      UserService userService) {
        this.docSystemService = docSystemService;
        this.userService = userService;
    }

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
        return docSystemService.listTree(spaceId, Boolean.TRUE.equals(includeDeleted));
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
        return docSystemService.renameSpace(id, name);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocSpace(@Argument Long id) {
        return docSystemService.deleteSpace(id);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode createDocNode(@Argument DocNodeCreateInput input) {
        return docSystemService.createNode(input, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode updateDocNode(@Argument Long id,
                                 @Argument DocNodeUpdateInput input) {
        return docSystemService.updateNode(id, input);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public DocNode moveDocNode(@Argument Long id,
                               @Argument DocNodeMoveInput input) {
        return docSystemService.moveNode(id, input);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocNode(@Argument Long id) {
        return docSystemService.deleteNode(id);
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
}
