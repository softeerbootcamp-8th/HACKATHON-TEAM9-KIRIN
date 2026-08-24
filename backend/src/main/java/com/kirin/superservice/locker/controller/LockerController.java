package com.kirin.superservice.locker.controller;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.dto.request.ChangeLockStatusRequest;
import com.kirin.superservice.locker.dto.response.LockerLockStatusResponse;
import com.kirin.superservice.locker.service.LockerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;

    @GetMapping("/{lockerId}/lock-status")
    public LockerLockStatusResponse getLockStatus(@PathVariable Long lockerId) {
        Locker locker = lockerService.getLocker(lockerId);
        return LockerLockStatusResponse.fromEntity(locker);
    }

    @PatchMapping("/{lockerId}/lock-status")
    public LockerLockStatusResponse changeLockStatus(
            @PathVariable Long lockerId,
            @RequestBody @Valid ChangeLockStatusRequest request) {
        Locker locker = lockerService.changeLockStatus(lockerId, request.lockStatus());
        return LockerLockStatusResponse.fromEntity(locker);
    }
}
