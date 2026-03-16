package com.simpleblog.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleblog.common.utils.RedisUtils;
import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.dto.BlogSearchPage;
import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.dto.ReplyInput;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.Category;
import com.simpleblog.model.entity.Comment;
import com.simpleblog.model.entity.Tag;
import com.simpleblog.model.entity.User;
import com.simpleblog.model.enums.CommentStatus;
import com.simpleblog.security.JwtService;
import com.simpleblog.service.EmailService;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.BlogService;
import com.simpleblog.service.BlogMetricsService;
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
    private final EmailService emailService;
    private final BlogMetricsService blogMetricsService;
    private final JwtService  jwtService;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    public BlogGraphqlController(BlogService blogService,
                                 CategoryService categoryService,
                                 TagService tagService,
                                 CommentService commentService,
                                 UserService userService,
                                 EmailService emailService,
                                 BlogMetricsService blogMetricsService,
                                 RedisUtils redisUtils,
                                 ObjectMapper objectMapper,
                                 JwtService jwtService) {
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.userService = userService;
        this.emailService = emailService;
        this.blogMetricsService = blogMetricsService;
        this.redisUtils = redisUtils;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
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
    public BlogSearchPage searchBlogsEs(@Argument String query, @Argument int page, @Argument int size) {
        return blogService.searchBlogsEs(query, page, size);
    }

    @QueryMapping
    public Blog blog(@Argument Long id) {
        return blogService.findById(id);
    }

    @QueryMapping
    public List<Blog> hotBlogs(@Argument Integer limit) {
        int safeLimit = limit == null ? 6 : limit;
        return blogService.listHotBlogs(safeLimit);
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
    public List<Comment> adminComments(@Argument Long blogId, @Argument CommentStatus status, @Argument String keyword) {
        return commentService.listByBlogId(blogId, status, keyword);
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @MutationMapping
    public Blog createBlog(@Argument BlogInput input) {
        /*User user = currentUser();
        return blogService.createBlog(user.getId(), input);*/
        Long userId = jwtService.currentUserId();
        return blogService.createBlog(userId, input);
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
    public Boolean recordBlogView(@Argument Long id) {
        Blog blog = blogService.findById(id);
        if (blog == null) {
            return false;
        }
        long fallback = blog.getViews() == null ? 0L : blog.getViews();
        blogMetricsService.getTotalViews(id, fallback);
        blogMetricsService.trackView(id);
        return true;
    }

    @MutationMapping
    public Blog voteBlog(@Argument Long id, @Argument int value) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        return blogService.voteBlog(id, value, user.map(User::getId).orElse(null), ip);
    }

    @MutationMapping
    public Comment createComment(@Argument CommentInput input) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : null;
        return commentService.create(user.map(User::getId).orElse(null), input, ip, ua);
    }

    @MutationMapping
    public Comment createReply(@Argument ReplyInput input) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : null;
        Comment reply = commentService.createReply(
                user.map(User::getId).orElse(null),
                input.commentId(),
                new CommentInput(null, null, input.content(), input.authorName(), input.authorEmail(), input.authorWebsite()),
                ip,
                ua
        );
        notifyReply(reply);
        return reply;
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Comment updateCommentStatus(@Argument Long id, @Argument CommentStatus status) {
        Comment comment = commentService.updateStatus(id, status);
        if (status == CommentStatus.approved) {
            notifyApproved(comment);
        }
        return comment;
    }

    @MutationMapping
    public Comment voteComment(@Argument Long id, @Argument int value) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        return commentService.vote(id, value, user.map(User::getId).orElse(null), ip);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean deleteComment(@Argument Long id) {
        return commentService.delete(id);
    }

    @PreAuthorize("hasRole('admin')")
    @MutationMapping
    public Boolean batchUpdateCommentStatus(@Argument List<Long> ids, @Argument CommentStatus status) {
        return commentService.batchUpdateStatus(ids, status);
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

    @SchemaMapping(typeName = "Blog", field = "views")
    public Long views(Blog blog) {
        long fallback = blog.getViews() == null ? 0L : blog.getViews();
        return blogMetricsService.getTotalViews(blog.getId(), fallback);
    }

    @SchemaMapping(typeName = "Blog", field = "likes")
    public Long likes(Blog blog) {
        long fallback = blog.getLikes() == null ? 0L : blog.getLikes();
        return blogService.getLikes(blog.getId(), fallback);
    }

    @SchemaMapping(typeName = "Blog", field = "dislikes")
    public Long dislikes(Blog blog) {
        long fallback = blog.getDislikes() == null ? 0L : blog.getDislikes();
        return blogService.getDislikes(blog.getId(), fallback);
    }

    @SchemaMapping(typeName = "Blog", field = "userVote")
    public Integer userVote(Blog blog) {
        Optional<User> user = SecurityUtils.currentUsername().map(userService::findByUsername);
        HttpServletRequest request = currentRequest();
        String ip = resolveClientIp(request);
        return blogService.getUserVote(blog.getId(), user.map(User::getId).orElse(null), ip);
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

    @SchemaMapping(typeName = "Comment", field = "replies")
    public List<Comment> replies(Comment comment) {
        if (comment.getId() == null) {
            return List.of();
        }
        return commentService.listByBlogId(comment.getBlogId(), CommentStatus.approved).stream()
                .filter(item -> comment.getId().equals(item.getParentId()))
                .toList();
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

    private void notifyApproved(Comment comment) {
        if (comment == null) {
            return;
        }
        if (comment.getAuthorEmail() == null || comment.getAuthorEmail().isBlank()) {
            return;
        }
        emailService.send(
                comment.getAuthorEmail(),
                "评论已通过审核",
                "你的评论已通过审核并显示在页面中。"
        );
    }

    private void notifyReply(Comment reply) {
        if (reply == null || reply.getParentId() == null) {
            return;
        }
        Comment parent = commentService.listByBlogId(reply.getBlogId(), null).stream()
                .filter(item -> reply.getParentId().equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (parent == null) {
            return;
        }
        if (parent.getAuthorEmail() == null || parent.getAuthorEmail().isBlank()) {
            return;
        }
        emailService.send(
                parent.getAuthorEmail(),
                "你的评论有了新回复",
                "有人回复了你的评论，快去查看吧。"
        );
    }
}

