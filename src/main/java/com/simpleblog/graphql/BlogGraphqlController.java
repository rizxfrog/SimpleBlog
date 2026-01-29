package com.simpleblog.graphql;

import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.dto.BlogSearchPage;
import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.Category;
import com.simpleblog.model.entity.Comment;
import com.simpleblog.model.entity.Tag;
import com.simpleblog.model.entity.User;
import com.simpleblog.model.enums.CommentStatus;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.BlogService;
import com.simpleblog.service.CategoryService;
import com.simpleblog.service.CommentService;
import com.simpleblog.service.TagService;
import com.simpleblog.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Controller
public class BlogGraphqlController {
    private final BlogService blogService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CommentService commentService;
    private final UserService userService;

    public BlogGraphqlController(BlogService blogService,
                                 CategoryService categoryService,
                                 TagService tagService,
                                 CommentService commentService,
                                 UserService userService) {
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.userService = userService;
    }

    @QueryMapping
    public BlogPage blogs(@Argument int page, @Argument int size, @Argument Boolean publishedOnly) {
        boolean onlyPublished = publishedOnly == null || publishedOnly;
        return blogService.listBlogs(page, size, onlyPublished);
    }

    @QueryMapping
    public BlogSearchPage searchBlogs(@Argument String query, @Argument int page, @Argument int size) {
        return blogService.searchBlogs(query, page, size);
    }

    @QueryMapping
    public Blog blog(@Argument Long id) {
        return blogService.findById(id);
    }

    @QueryMapping
    public List<Category> categories() {
        return categoryService.listAll();
    }

    @QueryMapping
    public List<Tag> tags() {
        return tagService.listAll();
    }

    @QueryMapping
    public List<Comment> comments(@Argument Long blogId) {
        return commentService.listApprovedByBlogId(blogId);
    }

    @PreAuthorize("hasRole('admin')")
    @QueryMapping
    public List<Comment> adminComments(@Argument Long blogId, @Argument CommentStatus status) {
        return commentService.listByBlogId(blogId, status);
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @MutationMapping
    public Blog createBlog(@Argument BlogInput input) {
        User user = currentUser();
        return blogService.createBlog(user.getId(), input);
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @MutationMapping
    public Blog updateBlog(@Argument Long id, @Argument BlogInput input) {
        return blogService.updateBlog(id, input);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteBlog(@Argument Long id) {
        return blogService.deleteBlog(id);
    }

    @MutationMapping
    public Comment createComment(@Argument CommentInput input) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : null;
        return commentService.create(user.map(User::getId).orElse(null), input, ip, ua);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Comment updateCommentStatus(@Argument Long id, @Argument CommentStatus status) {
        return commentService.updateStatus(id, status);
    }

    @MutationMapping
    public Comment voteComment(@Argument Long id, @Argument int value) {
        return commentService.vote(id, value);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Category createCategory(@Argument String name, @Argument String slug) {
        return categoryService.create(name, slug);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Tag createTag(@Argument String name, @Argument String slug) {
        return tagService.create(name, slug);
    }

    @SchemaMapping(typeName = "Blog", field = "author")
    public User author(Blog blog) {
        return userService.findById(blog.getAuthorId());
    }

    @SchemaMapping(typeName = "Blog", field = "category")
    public Category category(Blog blog) {
        if (blog.getCategoryId() == null) {
            return null;
        }
        return categoryService.findById(blog.getCategoryId());
    }

    @SchemaMapping(typeName = "Blog", field = "tags")
    public List<Tag> tags(Blog blog) {
        List<Long> tagIds = blogService.findTagIds(blog.getId());
        return tagService.listByIds(tagIds);
    }

    @SchemaMapping(typeName = "Comment", field = "user")
    public User user(Comment comment) {
        if (comment.getUserId() == null) {
            return null;
        }
        return userService.findById(comment.getUserId());
    }

    private User currentUser() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .orElseThrow(() -> new IllegalStateException("User not authenticated."));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

