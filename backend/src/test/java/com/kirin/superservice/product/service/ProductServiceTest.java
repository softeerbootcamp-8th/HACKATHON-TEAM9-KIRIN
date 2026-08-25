package com.kirin.superservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.locker.exception.LockerAccessDeniedException;
import com.kirin.superservice.locker.exception.LockerNotAvailableException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.domain.MemberType;
import com.kirin.superservice.member.service.MemberService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.exception.InvalidProductStatusException;
import com.kirin.superservice.product.exception.SellerMismatchException;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.exception.ReservationExpiredException;
import com.kirin.superservice.product.repository.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long 판매자_ID = 1L;
    private static final Long 다른_회원_ID = 2L;

    @Mock
    ProductRepository productRepository;

    @Mock
    LockerService lockerService;

    @Mock
    MemberService memberService;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    ProductService productService;

    private RegisterProductRequest 등록요청() {
        return new RegisterProductRequest("아이패드", 300000L, "상태 좋음", null);
    }

    private Member 판매자() {
        return Member.builder()
                .loginId("seller")
                .password("password")
                .nickname("원기")
                .memberType(MemberType.REGISTERED)
                .build();
    }

    private Product 물품(Long id, ProductStatus status) {
        return new Product(id, status == ProductStatus.PREPARING ? null : 1L,
                "아이패드", 300000L, "상태 좋음", null, 판매자_ID, "원기", status, LocalDateTime.now());
    }

    @Test
    void 물품을_등록하면_사물함을_지정하지_않고_준비중으로_저장된다() {
        // given
        given(memberService.getById(판매자_ID)).willReturn(판매자());
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = productService.registerProduct(등록요청(), 판매자_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
        assertThat(result.getSellerMemberId()).isEqualTo(판매자_ID);
        assertThat(result.getSellerName()).isEqualTo("원기");
    }

    @Test
    void 사물함이_재사용되어_같은_lockerId를_가진_물품이_여러_건이면_가장_최근_물품만_반환한다() {
        // given
        Product 이전_물품 = new Product(1L, 1L, "구 물품", 10000L, null, null,
                판매자_ID, "원기", ProductStatus.SOLD, LocalDateTime.of(2026, 8, 20, 10, 0));
        Product 최신_물품 = new Product(2L, 1L, "새 물품", 20000L, null, null,
                다른_회원_ID, "재훈", ProductStatus.RESERVED, LocalDateTime.of(2026, 8, 25, 10, 0));
        given(productRepository.findAllByLockerIdIsNotNull())
                .willReturn(List.of(이전_물품, 최신_물품));

        // when
        Map<Long, Product> result = productService.findAllProductsByLockerId();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(1L)).isEqualTo(최신_물품);
    }

    @Test
    void 물품보관함_번호로_지금_그_안에_있는_물품을_조회한다() {
        // given
        Product 물품 = 물품(1L, ProductStatus.SELLING);
        given(productRepository.findAllByLockerIdIsNotNull()).willReturn(List.of(물품));

        // when
        Product result = productService.getProductByLockerId(1L);

        // then
        assertThat(result).isEqualTo(물품);
    }

    @Test
    void 물품이_없는_물품보관함_번호로_조회하면_예외가_발생한다() {
        // given
        given(productRepository.findAllByLockerIdIsNotNull()).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> productService.getProductByLockerId(1L))
                .isInstanceOf(ProductNotFoundException.class);
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
        given(productRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ProductStatus.SELLING))
                .willReturn(List.of(물품(1L, ProductStatus.SELLING), 물품(2L, ProductStatus.SELLING)));

        // when
        List<Product> result = productService.findAllProductsByStatus(ProductStatus.SELLING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Product::isSelling);
    }

    @Test
    void 판매자_회원ID로_물품목록을_조회하면_상태와_관계없이_최신순으로_반환한다() {
        // given
        List<Product> products = List.of(물품(2L, ProductStatus.PREPARING), 물품(1L, ProductStatus.SOLD));
        given(productRepository.findAllBySellerMemberIdOrderByCreatedAtDescIdDesc(판매자_ID))
                .willReturn(products);

        // when
        List<Product> result = productService.findAllProductsBySellerMemberId(판매자_ID, null);

        // then
        assertThat(result).containsExactlyElementsOf(products);
    }

    @Test
    void 판매자_회원ID와_상태로_물품목록을_조회하면_해당상태를_최신순으로_반환한다() {
        // given
        List<Product> products = List.of(물품(2L, ProductStatus.RESERVED), 물품(1L, ProductStatus.RESERVED));
        given(productRepository.findAllBySellerMemberIdAndStatusOrderByCreatedAtDescIdDesc(
                판매자_ID, ProductStatus.RESERVED)).willReturn(products);

        // when
        List<Product> result = productService.findAllProductsBySellerMemberId(판매자_ID, ProductStatus.RESERVED);

        // then
        assertThat(result).containsExactlyElementsOf(products);
    }

    @Test
    void 준비중인_물품과_사용가능한_사물함을_지정하면_4시간_예약된다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.reserveLocker(1L, new ReserveLockerRequest(1L), 판매자_ID);

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
        assertThatThrownBy(() -> productService.reserveLocker(1L, new ReserveLockerRequest(1L), 판매자_ID))
                .isInstanceOf(LockerNotAvailableException.class);
    }

    @Test
    void 다른_회원이_예약하면_예외가_발생한다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.reserveLocker(1L, new ReserveLockerRequest(1L), 다른_회원_ID))
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
        Product result = productService.cancelLockerReservation(1L, 판매자_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
        assertThat(result.getReservationExpiresAt()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 다른_회원이_예약을_취소하면_예외가_발생한다() {
        // given
        Product product = 물품(1L, ProductStatus.PREPARING);
        product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 12, 0),
                LocalDateTime.of(2026, 8, 25, 16, 0));
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.cancelLockerReservation(1L, 다른_회원_ID))
                .isInstanceOf(SellerMismatchException.class);
    }

    @Test
    void 유효한_예약의_투입을_시작하면_사물함이_열린다() {
        // given
        Product product = 예약된물품();
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.startDeposit(1L, 판매자_ID);

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
        Product result = productService.completeDeposit(1L, 판매자_ID);

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
        assertThatThrownBy(() -> productService.startDeposit(1L, 판매자_ID))
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
        Product result = productService.startRecovery(1L, 판매자_ID);

        // then
        assertThat(result.getRecoveryStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 판매기간이_남은_물품도_회수를_시작하면_즉시_만료처리되고_사물함이_열린다() {
        // given
        Product product = 판매중물품();
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        Product result = productService.startRecovery(1L, 판매자_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.EXPIRED);
        assertThat(result.getRecoveryStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 예약중이거나_준비중인_물품의_회수를_시작하면_예외가_발생한다() {
        // given
        Product product = 예약된물품();
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.startRecovery(1L, 판매자_ID))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    void 다른_회원이_회수를_시작하면_예외가_발생한다() {
        // given
        Product product = 만료된물품();
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.startRecovery(1L, 다른_회원_ID))
                .isInstanceOf(SellerMismatchException.class);
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
        Product result = productService.completeRecovery(1L, 판매자_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(result.getLockerId()).isNull();
        assertThat(result.getSellingExpiresAt()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 물품보관함을_사용중인_판매자면_잠금상태_변경이_허용된다() {
        // given
        Product product = 만료된물품();
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));

        // when & then
        productService.validateLockerSeller(1L, 판매자_ID);
    }

    @Test
    void 물품보관함을_사용중인_판매자가_아니면_잠금상태_변경이_거부된다() {
        // given
        Product product = 만료된물품();
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.validateLockerSeller(1L, 다른_회원_ID))
                .isInstanceOf(SellerMismatchException.class);
    }

    @Test
    void 판매중인_물품이_없는_물품보관함의_잠금상태_변경은_거부된다() {
        // given
        given(productRepository.findByLockerId(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.validateLockerSeller(1L, 판매자_ID))
                .isInstanceOf(LockerAccessDeniedException.class);
    }

    @Test
    void 데모_잠금으로_예약된_물품은_투입_없이_바로_판매중이_된다() {
        // given
        Product product = 예약된물품();
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED);
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.completeDepositForDemo(1L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
        assertThat(product.getSellingStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 12, 0));
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.OCCUPIED);
    }

    @Test
    void 데모_잠금_대상_사물함에_예약된_물품이_없으면_아무일도_일어나지_않는다() {
        // given
        given(productRepository.findByLockerId(1L)).willReturn(Optional.empty());

        // when & then
        productService.completeDepositForDemo(1L);
    }

    @Test
    void 데모_잠금_대상_사물함의_물품이_이미_판매중이면_아무일도_일어나지_않는다() {
        // given
        Product product = 판매중물품();
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));

        // when
        productService.completeDepositForDemo(1L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    void 데모_잠금으로_회수시작된_물품은_바로_회수완료되고_사물함이_비워진다() {
        // given
        Product product = 만료된물품();
        product.startRecovery(LocalDateTime.of(2026, 8, 25, 11, 59));
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.completeRecoveryForDemo(1L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(product.getLockerId()).isNull();
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
    }

    @Test
    void 데모_잠금_대상_사물함에_회수시작된_물품이_없으면_아무일도_일어나지_않는다() {
        // given
        given(productRepository.findByLockerId(1L)).willReturn(Optional.empty());

        // when & then
        productService.completeRecoveryForDemo(1L);
    }

    @Test
    void 데모_잠금_대상_사물함의_물품이_아직_회수시작_전이면_아무일도_일어나지_않는다() {
        // given
        Product product = 만료된물품();
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));

        // when
        productService.completeRecoveryForDemo(1L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.EXPIRED);
    }

    @Test
    void 관리자가_초기화하면_판매중이던_물품이_준비중으로_복구되고_사물함이_비워진다() {
        // given
        Product product = 판매중물품();
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        given(productRepository.findByLockerId(1L)).willReturn(Optional.of(product));
        given(productRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.resetLockerForAdmin(1L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPARING);
        assertThat(product.getLockerId()).isNull();
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
    }

    @Test
    void 관리자가_비어있는_사물함을_초기화해도_잠금_비어있음_상태가_유지된다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(productRepository.findByLockerId(1L)).willReturn(Optional.empty());
        given(lockerService.getLockerForUpdate(1L)).willReturn(locker);

        // when
        productService.resetLockerForAdmin(1L);

        // then
        assertThat(locker.getLockStatus()).isEqualTo(LockStatus.LOCKED);
        assertThat(locker.getUsageStatus()).isEqualTo(UsageStatus.AVAILABLE);
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
