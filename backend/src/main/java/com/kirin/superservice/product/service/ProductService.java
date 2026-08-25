package com.kirin.superservice.product.service;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.exception.LockerAccessDeniedException;
import com.kirin.superservice.locker.exception.LockerNotAvailableException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.service.MemberService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.exception.InvalidProductStatusException;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.exception.ReservationExpiredException;
import com.kirin.superservice.product.exception.SellerMismatchException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final long SELLING_DAYS = 7;

    private final ProductRepository productRepository;
    private final LockerService lockerService;
    private final MemberService memberService;
    private final Clock clock;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    public List<Product> findAllProductsByStatus(ProductStatus status) {
        return productRepository.findAllByStatusOrderByCreatedAtDescIdDesc(status);
    }

    /** 세션 회원이 등록한 물품을 최신 등록순으로 조회한다. 상태를 지정하지 않으면 전체를 반환한다. */
    public List<Product> findAllProductsBySellerMemberId(Long sellerMemberId, ProductStatus status) {
        if (status == null) {
            return productRepository.findAllBySellerMemberIdOrderByCreatedAtDescIdDesc(sellerMemberId);
        }
        return productRepository.findAllBySellerMemberIdAndStatusOrderByCreatedAtDescIdDesc(sellerMemberId, status);
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
    public Product registerProduct(RegisterProductRequest request, Long sellerMemberId) {
        Member seller = memberService.getById(sellerMemberId);
        Product product = productRepository.save(new Product(
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                sellerMemberId,
                seller.getNickname()));
        log.info("물품 목록 등록 완료 - productId={}, sellerMemberId={}",
                product.getId(), product.getSellerMemberId());
        return product;
    }

    /** 물품 1건을 지정해 사용 가능한 물품보관함을 4시간 동안 예약한다. */
    @Transactional
    public Product reserveLocker(Long productId, ReserveLockerRequest request, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
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
    public Product cancelLockerReservation(Long productId, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
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

    /** 예약한 물품보관함을 열어 물품 투입을 시작한다. */
    @Transactional
    public Product startDeposit(Long productId, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
        LocalDateTime now = LocalDateTime.now(clock);
        validateReservable(product, now);
        if (product.hasStartedDeposit()) {
            return product;
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.startDeposit(now);
        locker.changeLockStatus(LockStatus.UNLOCKED);
        log.info("물품 투입 시작 - productId={}, lockerId={}, sellerName={}",
                productId, locker.getId(), product.getSellerName());
        return product;
    }

    /** 물품 투입을 완료하고 사물함을 잠근 뒤 7일 판매를 시작한다. */
    @Transactional
    public Product completeDeposit(Long productId, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
        LocalDateTime now = LocalDateTime.now(clock);
        validateReservable(product, now);
        if (!product.hasStartedDeposit()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.completeDeposit(now, now.plusDays(SELLING_DAYS));
        locker.occupy();
        locker.changeLockStatus(LockStatus.LOCKED);
        log.info("물품 투입 완료 - productId={}, lockerId={}, sellerName={}",
                productId, locker.getId(), product.getSellerName());
        return product;
    }

    /**
     * 판매 물품을 회수하기 위해 물품보관함 문을 연다. 판매기간이 이미 만료됐거나, 판매자가
     * 판매를 조기 종료하는 경우(아직 SELLING) 모두 허용한다. 조기 종료면 곧바로 판매기간을
     * 만료 처리해 이후 흐름(회수 완료 등)이 자연 만료와 동일하게 진행되게 한다.
     */
    @Transactional
    public Product startRecovery(Long productId, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
        if (!product.isSelling() && !product.isExpired()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }
        if (product.hasStartedRecovery()) {
            return product;
        }
        if (product.isSelling()) {
            product.expireSelling();
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.startRecovery(LocalDateTime.now(clock));
        locker.changeLockStatus(LockStatus.UNLOCKED);
        log.info("판매 물품 회수 시작 - productId={}, lockerId={}, sellerName={}",
                productId, locker.getId(), product.getSellerName());
        return product;
    }

    /** 판매자가 회수한 물품을 목록의 예약 가능 상태로 되돌린다. */
    @Transactional
    public Product completeRecovery(Long productId, Long sellerMemberId) {
        Product product = getProductForUpdate(productId);
        validateSeller(product, sellerMemberId);
        if (!product.isExpired() || !product.hasStartedRecovery()) {
            throw new InvalidProductStatusException(productId, product.getStatus());
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.completeRecovery();
        locker.changeLockStatus(LockStatus.LOCKED);
        locker.release();
        log.info("판매 만료 물품 회수 완료 - productId={}, lockerId={}, sellerName={}",
                productId, locker.getId(), product.getSellerName());
        return product;
    }

    /** 예약 만료 후보를 잠금 조회해 유효한 예약만 해제한다. */
    @Transactional
    public void expireLockerReservation(Long productId, LocalDateTime now) {
        Product product = getProductForUpdate(productId);
        if (!product.isReserved() || !product.isReservationExpiredAt(now)) {
            return;
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.cancelLockerReservation();
        locker.changeLockStatus(LockStatus.LOCKED);
        locker.release();
    }

    /** 판매 만료 후보를 잠금 조회해 유효한 판매만 회수 대기 상태로 바꾼다. */
    @Transactional
    public void expireSellingProduct(Long productId, LocalDateTime now) {
        Product product = getProductForUpdate(productId);
        if (!product.isSelling() || !product.isSellingExpiredAt(now)) {
            return;
        }

        Locker locker = lockerService.getLockerForUpdate(product.getLockerId());
        product.expireSelling();
        locker.occupy();
        locker.changeLockStatus(LockStatus.LOCKED);
    }

    /**
     * 물품보관함 현황 조회용으로, 사물함을 점유 중인 물품을 lockerId로 매핑해 조회한다.
     * lockerId는 물품이 회수·수령완료된 뒤에도 지워지지 않고 이력으로 남기 때문에, 같은
     * lockerId를 가진 물품이 여러 건 존재할 수 있다. 그중 가장 최근에 등록된 물품이 현재
     * 그 사물함을 점유 중인 물품이다(사물함이 비워진 뒤에만 재예약이 가능하므로).
     */
    public Map<Long, Product> findAllProductsByLockerId() {
        return productRepository.findAllByLockerIdIsNotNull().stream()
                .collect(Collectors.toMap(
                        Product::getLockerId,
                        product -> product,
                        (existing, latest) -> existing.getCreatedAt().isAfter(latest.getCreatedAt())
                                ? existing
                                : latest));
    }

    /** QR로 스캔한 물품보관함 번호로 지금 그 안에 있는 물품을 조회한다. 구매자가 로그인 없이도 호출한다. */
    public Product getProductByLockerId(Long lockerId) {
        Product product = findAllProductsByLockerId().get(lockerId);
        if (product == null) {
            throw ProductNotFoundException.byLockerId(lockerId);
        }
        return product;
    }

    /** 물품보관함을 지금 사용 중인 물품의 판매자인지 확인한다. 사물함 잠금 상태를 직접 조작할 때 사용한다. */
    public void validateLockerSeller(Long lockerId, Long sellerMemberId) {
        Product product = productRepository.findFirstByLockerIdOrderByCreatedAtDescIdDesc(lockerId)
                .orElseThrow(() -> new LockerAccessDeniedException(lockerId));
        validateSeller(product, sellerMemberId);
    }

    /**
     * 데모용: 사물함 잠금 버튼만으로 예약 중인 물품의 투입 시작·완료를 한 번에 흉내 낸다.
     * 로그인·판매자 검증·투입 시작 여부와 무관하게, 예약된 물품이면 바로 판매중으로 전환한다.
     */
    @Transactional
    public void completeDepositForDemo(Long lockerId) {
        productRepository.findFirstByLockerIdOrderByCreatedAtDescIdDesc(lockerId).ifPresent(found -> {
            if (!found.isReserved()) {
                return;
            }
            Product product = getProductForUpdate(found.getId());
            Locker locker = lockerService.getLockerForUpdate(lockerId);
            LocalDateTime now = LocalDateTime.now(clock);
            product.completeDeposit(now, now.plusDays(SELLING_DAYS));
            locker.occupy();
            log.info("데모용 사물함 잠금으로 판매 시작 - productId={}, lockerId={}", product.getId(), lockerId);
        });
    }

    /**
     * 데모용: 사물함 잠금 버튼만으로 회수 완료를 흉내 낸다. 판매자 본인 확인 없이,
     * 회수가 시작된(판매만료) 물품이 있으면 바로 회수 완료로 전환해 사물함을 비운다.
     */
    @Transactional
    public void completeRecoveryForDemo(Long lockerId) {
        productRepository.findFirstByLockerIdOrderByCreatedAtDescIdDesc(lockerId).ifPresent(found -> {
            if (!found.isExpired() || !found.hasStartedRecovery()) {
                return;
            }
            Product product = getProductForUpdate(found.getId());
            Locker locker = lockerService.getLockerForUpdate(lockerId);
            product.completeRecovery();
            locker.release();
            log.info("데모용 사물함 잠금으로 회수 완료 - productId={}, lockerId={}", product.getId(), lockerId);
        });
    }

    /**
     * 관리자용: 사물함을 초기 상태(잠김·비어있음)로 강제로 되돌린다. 사물함에 물려있는
     * 물품이 있으면 상태와 무관하게 회수 완료(PREPARING)로 되돌려 다시 등록·예약할 수 있게 한다.
     */
    @Transactional
    public void resetLockerForAdmin(Long lockerId) {
        productRepository.findFirstByLockerIdOrderByCreatedAtDescIdDesc(lockerId)
                .ifPresent(found -> getProductForUpdate(found.getId()).completeRecovery());
        Locker locker = lockerService.getLockerForUpdate(lockerId);
        locker.changeLockStatus(LockStatus.LOCKED);
        locker.release();
        log.info("관리자용 사물함 초기화 - lockerId={}", lockerId);
    }

    private void validateSeller(Product product, Long sellerMemberId) {
        if (!product.isOwnedBy(sellerMemberId)) {
            throw new SellerMismatchException(product.getId());
        }
    }

    private void validateReservable(Product product, LocalDateTime now) {
        if (!product.isReserved()) {
            throw new InvalidProductStatusException(product.getId(), product.getStatus());
        }
        if (product.isReservationExpiredAt(now)) {
            throw new ReservationExpiredException(product.getId());
        }
    }
}
