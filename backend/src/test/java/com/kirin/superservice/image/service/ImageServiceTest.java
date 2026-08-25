package com.kirin.superservice.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kirin.superservice.image.exception.InvalidImageFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageServiceTest {

    @TempDir
    Path tempDir;

    private ImageService imageService() {
        return new ImageService(tempDir.toString(), "/api/images");
    }

    @Test
    void 유효한_이미지를_저장하면_URL을_반환하고_파일이_저장된다() throws Exception {
        // given
        ImageService imageService = imageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.jpg", "image/jpeg", "dummy-image-bytes".getBytes());

        // when
        String imageUrl = imageService.storeImage(file);

        // then
        assertThat(imageUrl).startsWith("/api/images/").endsWith(".jpg");
        String storedFileName = imageUrl.substring("/api/images/".length());
        assertThat(Files.exists(tempDir.resolve(storedFileName))).isTrue();
    }

    @Test
    void 빈_파일을_저장하면_예외가_발생한다() {
        // given
        ImageService imageService = imageService();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        // when & then
        assertThatThrownBy(() -> imageService.storeImage(emptyFile))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void 지원하지_않는_형식이면_예외가_발생한다() {
        // given
        ImageService imageService = imageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "dummy".getBytes());

        // when & then
        assertThatThrownBy(() -> imageService.storeImage(file))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void 파일_크기가_초과하면_예외가_발생한다() {
        // given
        ImageService imageService = imageService();
        byte[] oversized = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", oversized);

        // when & then
        assertThatThrownBy(() -> imageService.storeImage(file))
                .isInstanceOf(InvalidImageFileException.class);
    }
}
