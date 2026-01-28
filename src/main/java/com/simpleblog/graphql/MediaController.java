package com.simpleblog.graphql;

import com.simpleblog.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private static final int DEFAULT_EXPIRES_SECONDS = 600;
    private final FileStorageService fileStorageService;

    public MediaController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/sign")
    public MediaSignResponse sign(@RequestBody MediaSignRequest request) {
        List<String> urls = request.urls() == null ? List.of() : request.urls();
        int expires = request.expiresSeconds() != null && request.expiresSeconds() > 0
                ? request.expiresSeconds()
                : DEFAULT_EXPIRES_SECONDS;
        Map<String, String> signedUrls = new HashMap<>();
        for (String url : urls) {
            String objectKey = fileStorageService.resolveObjectKey(url);
            if (objectKey == null) {
                continue;
            }
            String signed = fileStorageService.createSignedUrl(objectKey, Duration.ofSeconds(expires));
            signedUrls.put(url, signed);
        }
        return new MediaSignResponse(signedUrls, Instant.now().plusSeconds(expires).toString());
    }

    public record MediaSignRequest(List<String> urls, Integer expiresSeconds) {
    }

    public record MediaSignResponse(Map<String, String> signedUrls, String expiresAt) {
    }
}
