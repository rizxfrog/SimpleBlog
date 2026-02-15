package com.simpleblog.graphql;

import com.simpleblog.model.dto.DocumentCreateInput;
import com.simpleblog.model.dto.DocumentMoveInput;
import com.simpleblog.model.dto.DocumentSearchPage;
import com.simpleblog.model.dto.DocumentUpdateInput;
import com.simpleblog.model.entity.Document;
import com.simpleblog.model.entity.DocumentRevision;
import com.simpleblog.model.entity.User;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.DocumentRevisionService;
import com.simpleblog.service.DocumentService;
import com.simpleblog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DocumentGraphqlController {
    private final DocumentService documentService;
    private final DocumentRevisionService documentRevisionService;
    private final UserService userService;

    public DocumentGraphqlController(DocumentService documentService,
                                     DocumentRevisionService documentRevisionService,
                                     UserService userService) {
        this.documentService = documentService;
        this.documentRevisionService = documentRevisionService;
        this.userService = userService;
    }

    @QueryMapping
    public List<Document> documents(@Argument Boolean includeHidden,
                                    @Argument String project,
                                    @Argument String version) {
        boolean allowHidden = isAdmin();
        boolean include = allowHidden && Boolean.TRUE.equals(includeHidden);
        return documentService.listDocuments(include, project, version);
    }

    @QueryMapping
    public List<Document> documentTree(@Argument Long rootId,
                                       @Argument Boolean includeHidden,
                                       @Argument String project,
                                       @Argument String version) {
        boolean allowHidden = isAdmin();
        boolean include = allowHidden && Boolean.TRUE.equals(includeHidden);
        return documentService.documentTree(rootId, include, project, version);
    }

    @QueryMapping
    public Document document(@Argument Long id,
                             @Argument Boolean includeHidden,
                             @Argument String project,
                             @Argument String version) {
        boolean allowHidden = isAdmin();
        boolean include = allowHidden && Boolean.TRUE.equals(includeHidden);
        return documentService.findById(id, include, project, version);
    }

    @QueryMapping
    public DocumentSearchPage searchDocuments(@Argument String query,
                                              @Argument int page,
                                              @Argument int size,
                                              @Argument Boolean includeHidden,
                                              @Argument String project,
                                              @Argument String version) {
        boolean allowHidden = isAdmin();
        boolean include = allowHidden && Boolean.TRUE.equals(includeHidden);
        return documentService.searchDocuments(query, page, size, include, project, version);
    }

    @QueryMapping
    public List<String> documentProjects() {
        return documentService.listProjects();
    }

    @QueryMapping
    public List<String> documentVersions(@Argument String project) {
        return documentService.listVersions(project);
    }

    @PreAuthorize("hasRole('admin')")
    @QueryMapping
    public List<DocumentRevision> documentRevisions(@Argument Long documentId) {
        return documentRevisionService.listByDocumentId(documentId);
    }

    @PreAuthorize("hasRole('admin')")
    @QueryMapping
    public DocumentRevision documentRevision(@Argument Long id) {
        return documentRevisionService.findById(id);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Document restoreDocumentRevision(@Argument Long revisionId) {
        return documentRevisionService.restore(revisionId, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Document createDocument(@Argument DocumentCreateInput input) {
        return documentService.create(input, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Document updateDocument(@Argument Long id, @Argument DocumentUpdateInput input) {
        return documentService.update(id, input, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Document moveDocument(@Argument Long id, @Argument DocumentMoveInput input) {
        return documentService.move(id, input, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Document toggleDocumentHidden(@Argument Long id, @Argument Boolean hidden) {
        boolean nextHidden = Boolean.TRUE.equals(hidden);
        return documentService.setHidden(id, nextHidden, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocument(@Argument Long id) {
        return documentService.delete(id, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean createDocumentVersion(@Argument String sourceVersion,
                                         @Argument String project,
                                         @Argument String targetVersion) {
        return documentService.createVersion(project, sourceVersion, targetVersion, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocumentVersion(@Argument String project,
                                         @Argument String version) {
        return documentService.deleteVersion(project, version);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean createDocumentProject(@Argument String sourceProject,
                                         @Argument String targetProject) {
        return documentService.createProject(sourceProject, targetProject, currentUserId());
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteDocumentProject(@Argument String project) {
        return documentService.deleteProject(project);
    }

    private Long currentUserId() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .map(User::getId)
                .orElse(null);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
    }
}
