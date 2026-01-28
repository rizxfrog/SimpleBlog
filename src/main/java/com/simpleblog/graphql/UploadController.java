package com.simpleblog.graphql;

import com.simpleblog.service.FileStorageService;
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

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @PostMapping
    public UploadResult upload(@RequestParam("file") MultipartFile file) throws Exception {
        return fileStorageService.upload(file);
    }
}
