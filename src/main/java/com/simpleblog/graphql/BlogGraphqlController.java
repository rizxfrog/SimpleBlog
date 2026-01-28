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

import java.util.List;

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
        return commentService.listByBlogId(blogId);
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

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public Comment createComment(@Argument CommentInput input) {
        User user = currentUser();
        return commentService.create(user.getId(), input);
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
        return tagService.listAll().stream()
                .filter(tag -> tagIds.contains(tag.getId()))
                .toList();
    }

    @SchemaMapping(typeName = "Comment", field = "user")
    public User user(Comment comment) {
        return userService.findById(comment.getUserId());
    }

    private User currentUser() {
        return SecurityUtils.currentUsername()
                .map(userService::findByUsername)
                .orElseThrow(() -> new IllegalStateException("未登录"));
    }
}
