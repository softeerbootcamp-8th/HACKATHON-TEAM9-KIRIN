package com.kirin.superservice.locker.service;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.exception.LockerNotFoundException;
import com.kirin.superservice.locker.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LockerService {

    private final LockerRepository lockerRepository;

    public Locker getLocker(Long lockerId) {
        return lockerRepository.findById(lockerId)
                .orElseThrow(() -> new LockerNotFoundException(lockerId));
    }

    @Transactional
    public Locker changeLockStatus(Long lockerId, LockStatus lockStatus) {
        Locker locker = getLocker(lockerId);
        locker.changeLockStatus(lockStatus);
        log.info("보관함 잠금 상태 변경 - lockerId={}, lockStatus={}", lockerId, lockStatus);
        return locker;
    }
}
