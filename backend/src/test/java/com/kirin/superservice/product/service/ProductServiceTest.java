package com.kirin.superservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.locker.exception.LockerNotAvailableException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.CancelLockerReservationRequest;
import com.kirin.superservice.product.dto.request.CompleteDepositRequest;
import com.kirin.superservice.product.dto.request.CompleteRecoveryRequest;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.dto.request.StartDepositRequest;
import com.kirin.superservice.product.dto.request.StartRecoveryRequest;
import com.kirin.superservice.product.exception.SellerMismatchException;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.exception.ReservationExpiredException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    LockerService lockerService;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T03:00:00Z"), ZoneId.of("Asia/Seoul"));

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

    @Test
    void 준비중인_물품과_사용가능한_사물함을_지정하면_4시간_예약된다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.reserveLocker(1L, new ReserveLockerRequest(1L, "원기"));

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(result.getLockerId()).isEqualTo(1L);
        assertThat(result.getReservedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(result.getReservationExpiresAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 16, 0));
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.RESERVED);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 사용불가능한_사물함을_예약하면_예외가_발생한다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when & then
        assertThatThrownBy(() -> productService.reserveLocker(1L, new ReserveLockerRequest(1L, "원기")))
                .isInstanceOf(LockerNotAvailableException.class);
    }

    @Test
    void 다른_판매자가_예약하면_예외가_발생한다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.reserveLocker(1L, new ReserveLockerRequest(1L, "다른사람")))
                .isInstanceOf(SellerMismatchException.class);
    }

    @Test
    void 투입시작전_예약을_취소하면_물품과_사물함이_사용가능상태로_돌아간다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 12, 0),
                LocalDateTime.of(2026, 8, 25, 16, 0));
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.cancelLockerReservation(1L,
                new CancelLockerReservationRequest("원기"));

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
        assertThat(result.getReservationExpiresAt()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 유효한_예약의_투입을_시작하면_사물함이_열린다() {
        // given
        Product product = 예약된물품();
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.startDeposit(1L, new StartDepositRequest("원기"));

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(result.getDepositStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.RESERVED);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 투입을_완료하면_판매가_시작되고_사물함이_점유상태로_바뀐다() {
        // given
        Product product = 예약된물품();
        product.startDeposit(LocalDateTime.of(2026, 8, 25, 11, 59));
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.RESERVED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.completeDeposit(1L, new CompleteDepositRequest("원기"));

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.SELLING);
        assertThat(result.getSellingStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(result.getSellingExpiresAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 12, 0));
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.OCCUPIED);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 예약만료시각에_투입을_시작하면_예외가_발생한다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 8, 0),
                LocalDateTime.of(2026, 8, 25, 12, 0));
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.startDeposit(1L, new StartDepositRequest("원기")))
                .isInstanceOf(ReservationExpiredException.class);
    }

    @Test
    void 만료된_예약을_처리하면_물품과_사물함이_사용가능상태로_복구된다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 8, 0),
                LocalDateTime.of(2026, 8, 25, 12, 0));
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.RESERVED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.expireLockerReservation(1L, LocalDateTime.of(2026, 8, 25, 12, 0));

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(product.getLockerId()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 판매기간이_만료되면_회수대기상태가_되고_사물함점유는_유지된다() {
        // given
        Product product = 판매중물품();
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.expireSellingProduct(1L, LocalDateTime.of(2026, 8, 25, 12, 0));

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.EXPIRED);
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.OCCUPIED);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 만료된_물품의_회수를_시작하면_사물함이_열린다() {
        // given
        Product product = 만료된물품();
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.startRecovery(1L, new StartRecoveryRequest("원기"));

        // then
        assertThat(result.getRecoveryStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 회수를_완료하면_물품은_다시_예약가능하고_사물함은_해제된다() {
        // given
        Product product = 만료된물품();
        product.startRecovery(LocalDateTime.of(2026, 8, 25, 11, 59));
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.completeRecovery(1L, new CompleteRecoveryRequest("원기"));

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
        assertThat(result.getSellingExpiresAt()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    private Product 예약된물품() {
        Product product = 물품(1L, ProductStatus.PREPARING);
        product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 11, 0),
                LocalDateTime.of(2026, 8, 25, 16, 0));
        return product;
    }

    private Product 판매중물품() {
        Product product = 예약된물품();
        product.startDeposit(LocalDateTime.of(2026, 8, 18, 11, 59));
        product.completeDeposit(LocalDateTime.of(2026, 8, 18, 12, 0),
                LocalDateTime.of(2026, 8, 25, 12, 0));
        return product;
    }

    private Product 만료된물품() {
        Product product = 판매중물품();
        product.expireSelling();
        return product;
    }
}
