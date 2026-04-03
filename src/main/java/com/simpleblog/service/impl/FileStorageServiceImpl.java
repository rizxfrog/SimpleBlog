package com.simpleblog.service.impl;

import com.simpleblog.config.StorageProperties;
import com.simpleblog.mapper.FileObjectMapper;
import com.simpleblog.model.entity.FileObject;
import com.simpleblog.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final FileObjectMapper fileObjectMapper;
    private final AtomicBoolean bucketChecked = new AtomicBoolean(false);

    public FileStorageServiceImpl(
            S3Client s3Client,
            S3Presigner s3Presigner,
            StorageProperties properties,
            FileObjectMapper fileObjectMapper
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.fileObjectMapper = fileObjectMapper;
    }

    @Override
    public UploadResult upload(MultipartFile file, UploadContext context) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }
        String contentType = resolveContentType(file);
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new IllegalArgumentException("Only image or video files are supported.");
        }

        ensureBucket();
        String objectKey = buildObjectKey(file.getOriginalFilename(), context, contentType);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String url = buildPublicUrl(objectKey);

        FileObject record = new FileObject();
        record.setName(file.getOriginalFilename());
        record.setUrl(url);
        record.setContentType(contentType);
        record.setSizeBytes(file.getSize());
        record.setCreatedAt(LocalDateTime.now());
        fileObjectMapper.insert(record);

        return new UploadResult(url, file.getOriginalFilename(), contentType, file.getSize());
    }

    @Override
    public String createSignedUrl(String objectKey, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public String resolveObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String normalized = url.trim();
        String publicUrl = normalizeBase(properties.getPublicUrl());
        if (publicUrl != null && normalized.startsWith(publicUrl)) {
            return trimLeadingSlash(normalized.substring(publicUrl.length()));
        }
        String endpoint = normalizeBase(properties.getEndpoint());
        if (endpoint != null) {
            String base = endpoint + "/" + properties.getBucket();
            if (normalized.startsWith(base)) {
                return trimLeadingSlash(normalized.substring(base.length()));
            }
        }
        if (!normalized.contains("://")) {
            return trimLeadingSlash(normalized);
        }
        return null;
    }

    private void ensureBucket() {
        if (bucketChecked.get()) {
            return;
        }
        synchronized (bucketChecked) {
            if (bucketChecked.get()) {
                return;
            }
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
            } catch (NoSuchBucketException ex) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            } catch (S3Exception ex) {
                if (ex.statusCode() == 404) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
                } else {
                    throw ex;
                }
            }
            bucketChecked.set(true);
        }
    }

    private String buildObjectKey(String originalName, UploadContext context, String contentType) {
        String datePath = LocalDate.now().toString();
        String userSegment = sanitizeSegment(context.username() == null ? "anonymous" : context.username());
        String categorySegment = sanitizeSegment(context.category() == null ? "uncategorized" : context.category());
        String blogSegment = context.blogId() == null ? null : "post-" + context.blogId();
        String kindSegment = resolveKindSegment(contentType);
        String extension = "";
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                extension = originalName.substring(dotIndex);
            }
        }
        StringBuilder key = new StringBuilder("uploads/");
        key.append(userSegment).append("/");
        key.append(categorySegment).append("/");
        key.append(kindSegment).append("/");
        if (blogSegment != null) {
            key.append(blogSegment).append("/");
        }
        key.append(datePath).append("/").append(UUID.randomUUID()).append(extension);
        return key.toString();
    }

    private String buildPublicUrl(String objectKey) {
        String publicUrl = properties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl.replaceAll("/+$", "") + "/" + objectKey;
        }
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        return endpoint + "/" + properties.getBucket() + "/" + objectKey;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            return URLConnection.guessContentTypeFromName(name);
        }
        return null;
    }

    private String normalizeBase(String base) {
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.replaceAll("/+$", "");
    }

    private String trimLeadingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("^/+", "");
    }

    private String sanitizeSegment(String value) {
        String normalized = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "misc" : normalized;
    }

    private String resolveKindSegment(String contentType) {
        if (contentType == null) {
            return "files";
        }
        if (contentType.startsWith("image/")) {
            return "images";
        }
        if (contentType.startsWith("video/")) {
            return "videos";
        }
        return "files";
    }
}
