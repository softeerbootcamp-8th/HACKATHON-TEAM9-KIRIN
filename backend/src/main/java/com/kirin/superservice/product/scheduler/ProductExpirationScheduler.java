package com.kirin.superservice.product.scheduler;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.repository.ProductRepository;
import com.kirin.superservice.product.service.ProductService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductExpirationScheduler {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${kirin.expiration.fixed-delay}")
    public void expireProducts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Product> reservationProducts = productRepository
                .findAllByStatusAndReservationExpiresAtLessThanEqual(ProductStatus.RESERVED, now);
        List<Product> sellingProducts = productRepository
                .findAllByStatusAndSellingExpiresAtLessThanEqual(ProductStatus.SELLING, now);
        reservationProducts
                .stream()
                .map(Product::getId)
                .forEach(productId -> productService.expireLockerReservation(productId, now));
        sellingProducts
                .stream()
                .map(Product::getId)
                .forEach(productId -> productService.expireSellingProduct(productId, now));
        if (!reservationProducts.isEmpty() || !sellingProducts.isEmpty()) {
            log.info("물품 만료 처리 완료 - reservationCount={}, sellingCount={}",
                    reservationProducts.size(), sellingProducts.size());
        }
    }
}
