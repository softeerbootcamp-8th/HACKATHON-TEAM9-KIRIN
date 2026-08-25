package com.kirin.superservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private RegisterProductRequest 등록요청() {
        return new RegisterProductRequest("아이패드", 300000L, "상태 좋음", null, "원기");
    }

    private Product 물품(Long id, ProductStatus status) {
        return new Product(id, status == ProductStatus.PREPARING ? null : 1L,
                "아이패드", 300000L, "상태 좋음", null, "원기", status, LocalDateTime.now());
    }

    @Test
    void 물품을_등록하면_사물함을_지정하지_않고_준비중으로_저장된다() {
        // given
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = productService.registerProduct(등록요청());

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
    }

    @Test
    void 존재하지_않는_물품을_조회하면_예외가_발생한다() {
        // given
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 판매중인_물품_목록을_조회하면_해당_상태의_물품만_반환한다() {
        // given
        given(productRepository.findAllByStatusOrderByCreatedAtDesc(ProductStatus.SELLING))
                .willReturn(List.of(물품(1L, ProductStatus.SELLING), 물품(2L, ProductStatus.SELLING)));

        // when
        List<Product> result = productService.findAllProductsByStatus(ProductStatus.SELLING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Product::isSelling);
    }
}
