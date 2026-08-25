package com.kirin.superservice.product.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.repository.ProductRepository;
import com.kirin.superservice.product.service.ProductService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductExpirationSchedulerTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductService productService;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    ProductExpirationScheduler productExpirationScheduler;

    @Test
    void 예약과_판매_만료후보를_조회해_각각의_만료처리를_요청한다() {
        // given
        Product reservedProduct = new Product(1L, 1L, "아이패드", 300000L, "상태 좋음", null, "원기",
                ProductStatus.RESERVED, LocalDateTime.of(2026, 8, 25, 8, 0));
        Product sellingProduct = new Product(2L, 2L, "맥북", 500000L, "상태 좋음", null, "원기",
                ProductStatus.SELLING, LocalDateTime.of(2026, 8, 18, 12, 0));
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 12, 0);
        given(productRepository.findAllByStatusAndReservationExpiresAtLessThanEqual(
                ProductStatus.RESERVED, now)).willReturn(List.of(reservedProduct));
        given(productRepository.findAllByStatusAndSellingExpiresAtLessThanEqual(
                ProductStatus.SELLING, now)).willReturn(List.of(sellingProduct));

        // when
        productExpirationScheduler.expireProducts();

        // then
        then(productService).should().expireLockerReservation(1L, now);
        then(productService).should().expireSellingProduct(2L, now);
    }
}
