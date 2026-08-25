package com.kirin.superservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.locker.exception.LockerAlreadyOccupiedException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.exception.InvalidProductStatusException;
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

    @Mock
    LockerService lockerService;

    @InjectMocks
    ProductService productService;

    private RegisterProductRequest 등록요청() {
        return new RegisterProductRequest(1L, "아이패드", 300000L, "상태 좋음", null, "원기");
    }

    private Product 물품(Long id, ProductStatus status) {
        return new Product(id, 1L, "아이패드", 300000L, "상태 좋음", null, "원기",
                status, LocalDateTime.now());
    }

    @Test
    void 사용_가능한_보관함에_물품을_등록하면_보관함이_사용중으로_바뀌고_문이_열린다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);
        given(productRepository.save(any(Product.class))).willReturn(물품(1L, ProductStatus.PREPARING));

        // when
        productService.registerProduct(등록요청());

        // then
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.OCCUPIED);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 물품을_등록하면_준비중_상태로_저장된다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);
        given(productRepository.save(any(Product.class))).willReturn(물품(1L, ProductStatus.PREPARING));

        // when
        Product result = productService.registerProduct(등록요청());

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isEqualTo(1L);
    }

    @Test
    void 이미_사용중인_보관함에_물품을_등록하면_예외가_발생한다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when & then
        assertThatThrownBy(() -> productService.registerProduct(등록요청()))
                .isInstanceOf(LockerAlreadyOccupiedException.class);
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

    @Test
    void 준비중인_물품의_등록을_완료하면_판매중이_되고_보관함이_잠긴다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(lockerService.getLocker(1L)).willReturn(locker);

        // when
        Product result = productService.completeRegistration(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.SELLING);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 이미_판매중인_물품의_등록을_완료해도_판매중_상태가_유지된다() {
        // given
        Product product = 물품(1L, ProductStatus.SELLING);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        Product result = productService.completeRegistration(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    void 판매완료된_물품의_등록을_완료하려_하면_예외가_발생한다() {
        // given
        given(productRepository.findById(1L)).willReturn(Optional.of(물품(1L, ProductStatus.SOLD)));

        // when & then
        assertThatThrownBy(() -> productService.completeRegistration(1L))
                .isInstanceOf(InvalidProductStatusException.class);
    }
}
