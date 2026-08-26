package com.kirin.superservice.product.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 게스트 회원용 더미 상품 데이터를 resources/dummy-products.json에서 읽어온다.
 * 상품에 딸린 이미지는 resources/dummy-images/에서 실제 이미지 업로드 디렉터리로 복사해
 * 기존 이미지 서빙 경로({@code kirin.image.base-url})로 그대로 접근할 수 있게 한다.
 */
@Component
public class DummyProductLoader {

    private static final String DUMMY_PRODUCT_FILE = "dummy-products.json";
    private static final String DUMMY_IMAGE_DIR = "dummy-images";

    private final List<DummyProductData> dummyProducts;

    public DummyProductLoader(
            ObjectMapper objectMapper,
            @Value("${kirin.image.upload-dir}") String uploadDir,
            @Value("${kirin.image.base-url}") String imageBaseUrl) {
        this.dummyProducts = loadDummyProducts(objectMapper, Path.of(uploadDir), imageBaseUrl);
    }

    public List<DummyProductData> getDummyProducts() {
        return dummyProducts;
    }

    private List<DummyProductData> loadDummyProducts(ObjectMapper objectMapper, Path uploadDir, String imageBaseUrl) {
        return readSpecs(objectMapper).stream()
                .map(spec -> new DummyProductData(
                        spec.name(),
                        spec.price(),
                        spec.description(),
                        List.of(imageBaseUrl + "/" + copyImageToUploadDir(spec.imageFile(), uploadDir))))
                .toList();
    }

    private List<DummyProductSpec> readSpecs(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource(DUMMY_PRODUCT_FILE).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<DummyProductSpec>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("더미 상품 데이터 로드 실패 - file=" + DUMMY_PRODUCT_FILE, e);
        }
    }

    private String copyImageToUploadDir(String imageFile, Path uploadDir) {
        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = new ClassPathResource(DUMMY_IMAGE_DIR + "/" + imageFile).getInputStream()) {
                Files.copy(inputStream, uploadDir.resolve(imageFile), StandardCopyOption.REPLACE_EXISTING);
            }
            return imageFile;
        } catch (IOException e) {
            throw new IllegalStateException("더미 상품 이미지 복사 실패 - imageFile=" + imageFile, e);
        }
    }

    private record DummyProductSpec(String name, Long price, String description, String imageFile) {
    }

    public record DummyProductData(String name, Long price, String description, List<String> imageUrls) {
    }
}
