package com.kirin.superservice.product.service;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.exception.LockerNotAvailableException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.CancelLockerReservationRequest;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.exception.InvalidProductStatusException;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.exception.SellerMismatchException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.time.Clock;
import java.time.LocalDateTime;
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

    private static final long RESERVATION_HOURS = 4;

    private final ProductRepository productRepository;
    private final LockerService lockerService;
    private final Clock clock;

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

    /** 물품 1건을 지정해 사용 가능한 물품보관함을 4시간 동안 예약한다. */
    @Transactional
    public Product reserveLocker(Long productId, ReserveLockerRequest request) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, request.sellerName());
        if (!product.isPreparing()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }

        Locker locker = lockerService.getLockerForUpdate(request.lockerId());
        if (!locker.isAvailable()) {
            throw new LockerNotAvailableException(locker.getId());
        }

        LocalDateTime reservedAt = LocalDateTime.now(clock);
        product.reserveLocker(locker.getId(), reservedAt, reservedAt.plusHours(RESERVATION_HOURS));
        locker.reserve();
        locker.changeLockStatus(LockStatus.LOCKED);
        log.info("물품보관함 예약 완료 - productId={}, lockerId={}, sellerName={}",
                product.getId(), locker.getId(), product.getSellerName());
        return product;
    }

    /** 투입 시작 전의 예약을 취소하고 물품보관함을 다시 사용 가능 상태로 돌린다. */
    @Transactional
    public Product cancelLockerReservation(Long productId, CancelLockerReservationRequest request) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, request.sellerName());
        if (!product.isReserved() || product.hasStartedDeposit()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.cancelLockerReservation();
        locker.changeLockStatus(LockStatus.LOCKED);
        locker.release();
        log.info("물품보관함 예약 취소 - productId={}, lockerId={}, sellerName={}",
                productId, locker.getId(), product.getSellerName());
        return product;
    }

    private void validateSeller(Product product, String sellerName) {
        if (!product.isSeller(sellerName)) {
            throw new SellerMismatchException(product.getId());
        }
    }
}
