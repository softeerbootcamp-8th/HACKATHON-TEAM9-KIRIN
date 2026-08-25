package com.kirin.superservice.product.service;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.exception.LockerAlreadyOccupiedException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.exception.InvalidProductStatusException;
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
    private final LockerService lockerService;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public List<Product> findAllProductsByStatus(ProductStatus status) {
        return productRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 물품을 등록하고 보관함을 점유한 뒤 문을 연다. 판매자 둘이 같은 보관함을 동시에 고를 수 있어
     * 보관함 행을 잠근 채로 사용 여부를 확인한다.
     */
    @Transactional
    public Product registerProduct(RegisterProductRequest request) {
        Locker locker = lockerService.getLockerForUpdate(request.lockerId());
        if (!locker.isAvailable()) {
            throw new LockerAlreadyOccupiedException(locker.getId());
        }
        locker.occupy();
        locker.changeLockStatus(LockStatus.UNLOCKED);

        Product product = productRepository.save(new Product(
                request.lockerId(),
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.sellerName()));
        log.info("물품 등록 완료 - productId={}, lockerId={}, sellerName={}",
                product.getId(), product.getLockerId(), product.getSellerName());
        return product;
    }

    /**
     * 판매자가 물품을 다 넣었을 때 호출한다. 보관함을 잠그고 물품을 판매중으로 바꾼다.
     * 버튼을 두 번 눌러도 문제가 없도록 이미 판매중이면 그대로 둔다.
     */
    @Transactional
    public Product completeRegistration(Long productId) {
        Product product = getProduct(productId);
        if (product.isSelling()) {
            return product;
        }
        if (!product.isPreparing()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }
        product.startSelling();
        lockerService.getLocker(product.getLockerId()).changeLockStatus(LockStatus.LOCKED);
        log.info("물품 등록 마무리 - productId={}, lockerId={}", productId, product.getLockerId());
        return product;
    }
}
