package com.simpleblog.service;

import com.simpleblog.model.entity.FileObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

public interface FileStorageService {
    /**
     * 上传文件
     * @param file 文件
     * @param context 上传上下文
     * @return 上传结果
     * @throws IOException IO异常
     */
    UploadResult upload(MultipartFile file, UploadContext context) throws IOException;

    /**
     * 创建签名URL
     * @param objectKey 对象键
     * @param duration 有效期
     * @return 签名URL
     */
    String createSignedUrl(String objectKey, Duration duration);

    /**
     * 解析对象键
     * @param url URL
     * @return 对象键
     */
    String resolveObjectKey(String url);

    record UploadResult(String url, String name, String contentType, long sizeBytes) {}
    record UploadContext(String username, String category, Long blogId) {}
}
