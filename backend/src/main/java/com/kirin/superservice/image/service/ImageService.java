package com.kirin.superservice.image.service;

import com.kirin.superservice.image.exception.ImageStorageException;
import com.kirin.superservice.image.exception.InvalidImageFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ImageService {

    private static final Map<String, String> ALLOWED_CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final Path uploadDir;
    private final String baseUrl;

    public ImageService(
            @Value("${kirin.image.upload-dir}") String uploadDir,
            @Value("${kirin.image.base-url}") String baseUrl) {
        this.uploadDir = Path.of(uploadDir);
        this.baseUrl = baseUrl;
    }

    public String storeImage(MultipartFile file) {
        String extension = validateAndGetExtension(file);

        String storedFileName = UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(storedFileName);

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("이미지 저장 실패 - fileName={}", storedFileName, e);
            throw new ImageStorageException(storedFileName, e);
        }

        log.info("이미지 저장 완료 - fileName={}, sizeBytes={}", storedFileName, file.getSize());
        return baseUrl + "/" + storedFileName;
    }

    private String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageFileException("파일이 비어있습니다");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidImageFileException("파일 크기가 10MB를 초과합니다");
        }
        String extension = ALLOWED_CONTENT_TYPE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new InvalidImageFileException("지원하지 않는 파일 형식입니다 - contentType=" + file.getContentType());
        }
        return extension;
    }
}
