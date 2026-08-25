package com.kirin.superservice.locker.controller;

import com.kirin.superservice.global.auth.LoginMember;
import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.dto.request.ChangeLockStatusRequest;
import com.kirin.superservice.locker.dto.response.LockerListResponse;
import com.kirin.superservice.locker.dto.response.LockerLockStatusResponse;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET은 물품보관함 장치(ESP32)가 인증 없이 폴링하므로 {@code /api/lockers/**}를 통째로
 * 세션 인증에서 제외한다({@code WebConfig}). PATCH는 로그인한 회원이 그 사물함에 물품을
 * 두고 있는 판매자 본인일 때만 허용한다. 장치 전용 인증(예: API 키)은 팀 논의 후 별도로 도입한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;
    private final ProductService productService;

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

    @PatchMapping("/{lockerId}/lock-status")
    public LockerLockStatusResponse changeLockStatus(
            @LoginMember Long memberId,
            @PathVariable Long lockerId,
            @RequestBody @Valid ChangeLockStatusRequest request) {
        productService.validateLockerSeller(lockerId, memberId);
        Locker locker = lockerService.changeLockStatus(lockerId, request.lockStatus());
        log.info("사물함 잠금 상태 변경 - lockerId={}, lockStatus={}, memberId={}",
                lockerId, request.lockStatus(), memberId);
        return LockerLockStatusResponse.fromEntity(locker);
    }
}
