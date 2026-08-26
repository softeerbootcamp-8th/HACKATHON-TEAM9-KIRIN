package com.kirin.superservice.locker.controller;

import com.kirin.superservice.global.auth.LoginMember;
import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.dto.request.ChangeLockStatusRequest;
import com.kirin.superservice.locker.dto.response.LockerListResponse;
import com.kirin.superservice.locker.dto.response.LockerLockStatusResponse;
import com.kirin.superservice.locker.dto.response.LockerSummaryResponse;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.dto.response.ProductResponse;
import com.kirin.superservice.product.service.ProductService;
import com.kirin.superservice.transaction.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/lockers/**}는 GET·PATCH 모두 세션 인증에서 제외한다({@code WebConfig}).
 * GET은 물품보관함 장치(ESP32)가 인증 없이 폴링하기 위함이고, PATCH는 데모에서 로그인
 * 절차 없이 바로 사물함 잠금 상태를 조작할 수 있게 하기 위한 임시 조정이다. 열 때
 * (UNLOCKED) 그 사물함에 예약된 물품이 있으면 투입 절차 없이 곧바로 판매중으로 전환한다
 * (ESP32가 문이 실제로 닫혔는지 감지할 수 없어, 잠금 신호 대신 여는 시점을 기준으로 삼는다).
 * 잠글 때(LOCKED)는 회수가 시작된(판매만료) 물품이 있으면 곧바로 회수 완료로, 결제완료된
 * 거래가 있으면 곧바로 수령완료로 전환한다(모두 데모용).
 * 정식 인가(판매자·구매자 본인 확인) 또는 장치 전용 인증(예: API 키)은 팀 논의 후 별도로 도입한다.
 * POST .../reset은 관리자 페이지 전용으로, 사물함을 물품 상태와 무관하게 초기 상태(잠김·비어있음)로
 * 강제로 되돌린다. 안에 물려있던 물품은 회수 완료(PREPARING) 상태로 복구된다.
 */
@Slf4j
@RestController
@RequestMapping("/api/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;
    private final ProductService productService;
    private final TransactionService transactionService;

    @GetMapping
    public LockerListResponse getLockers(@LoginMember(required = false) Long loginMemberId) {
        List<Locker> lockers = lockerService.findAllLockers();
        Map<Long, Product> productsByLockerId = productService.findAllProductsByLockerId();
        return LockerListResponse.of(lockers, productsByLockerId, loginMemberId);
    }

    @GetMapping("/{lockerId}/lock-status")
    public LockerLockStatusResponse getLockStatus(@PathVariable Long lockerId) {
        Locker locker = lockerService.getLocker(lockerId);
        return LockerLockStatusResponse.fromEntity(locker);
    }

    /** 구매자가 물품보관함에 붙은 QR을 스캔해 지금 그 안에 있는 물품을 조회한다. 로그인이 필요 없다. */
    @GetMapping("/{lockerId}/product")
    public ProductResponse getProductByLocker(@PathVariable Long lockerId) {
        Product product = productService.getProductByLockerId(lockerId);
        return ProductResponse.fromEntity(product);
    }

    @PatchMapping("/{lockerId}/lock-status")
    public LockerLockStatusResponse changeLockStatus(
            @PathVariable Long lockerId,
            @RequestBody @Valid ChangeLockStatusRequest request) {
        Locker locker = lockerService.changeLockStatus(lockerId, request.lockStatus());
        if (request.lockStatus() == LockStatus.LOCKED) {
            productService.completeDepositForDemo(lockerId);
            productService.completeRecoveryForDemo(lockerId);
            transactionService.completePickupForDemo(lockerId);
        } else {
            productService.startSellingForDemo(lockerId);
        }
        log.info("사물함 잠금 상태 변경 - lockerId={}, lockStatus={}", lockerId, request.lockStatus());
        return LockerLockStatusResponse.fromEntity(locker);
    }

    @PostMapping("/{lockerId}/reset")
    public LockerSummaryResponse resetLocker(@PathVariable Long lockerId) {
        productService.resetLockerForAdmin(lockerId);
        Locker locker = lockerService.getLocker(lockerId);
        log.info("관리자용 사물함 초기화 - lockerId={}", lockerId);
        return LockerSummaryResponse.of(locker, null, null);
    }
}
