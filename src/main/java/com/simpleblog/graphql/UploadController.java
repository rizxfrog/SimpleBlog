package com.simpleblog.graphql;

import com.simpleblog.model.entity.Category;
import com.simpleblog.security.SecurityUtils;
import com.simpleblog.service.CategoryService;
import com.simpleblog.service.FileStorageService;
import com.simpleblog.service.FileStorageService.UploadContext;
import com.simpleblog.service.FileStorageService.UploadResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final FileStorageService fileStorageService;
    private final CategoryService categoryService;

    public UploadController(FileStorageService fileStorageService, CategoryService categoryService) {
        this.fileStorageService = fileStorageService;
        this.categoryService = categoryService;
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @PostMapping
    public UploadResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "blogId", required = false) Long blogId
    ) throws Exception {
        String username = SecurityUtils.currentUsername().orElse("anonymous");
        String categorySlug = resolveCategorySlug(categoryId);
        UploadContext context = new UploadContext(username, categorySlug, blogId);
        return fileStorageService.upload(file, context);
    }

    private String resolveCategorySlug(Long categoryId) {
        if (categoryId == null) {
            return "uncategorized";
        }
        Category category = categoryService.findById(categoryId);
        if (category == null) {
            return "uncategorized";
        }
        String slug = category.getSlug();
        if (slug != null && !slug.isBlank()) {
            return slug;
        }
        return category.getName() == null ? "uncategorized" : category.getName();
    }
}
