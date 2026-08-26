package com.kirin.superservice.product.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** 게스트 회원용 더미 상품 데이터를 resources/dummy-products.json에서 읽어온다. */
@Component
public class DummyProductLoader {

    private static final String DUMMY_PRODUCT_FILE = "dummy-products.json";

    private final List<DummyProductData> dummyProducts;

    public DummyProductLoader(ObjectMapper objectMapper) {
        this.dummyProducts = loadDummyProducts(objectMapper);
    }

    public List<DummyProductData> getDummyProducts() {
        return dummyProducts;
    }

    private List<DummyProductData> loadDummyProducts(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource(DUMMY_PRODUCT_FILE).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<DummyProductData>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("더미 상품 데이터 로드 실패 - file=" + DUMMY_PRODUCT_FILE, e);
        }
    }

    public record DummyProductData(String name, Long price, String description, String imageUrl) {
    }
}
