package com.kirin.superservice.product.service;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public List<Product> findAllProductsByStatus(ProductStatus status) {
        return productRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 물품 행을 잠근 채로 조회한다. 구매자 둘이 같은 물품을 동시에 사는 것을 막기 위한 것이라
     * 반드시 호출자의 쓰기 트랜잭션 안에서 사용한다.
     */
    @Transactional
    public Product getProductForUpdate(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    /** 물품 목록에만 등록한다. 사물함 예약과 물품 투입은 별도 흐름에서 처리한다. */
    @Transactional
    public Product registerProduct(RegisterProductRequest request) {
        Product product = productRepository.save(new Product(
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.sellerName()));
        log.info("물품 목록 등록 완료 - productId={}, sellerName={}",
                product.getId(), product.getSellerName());
        return product;
    }
}
