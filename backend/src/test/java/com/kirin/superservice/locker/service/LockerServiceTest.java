package com.kirin.superservice.locker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.locker.exception.LockerNotFoundException;
import com.kirin.superservice.locker.repository.LockerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LockerServiceTest {

    @Mock
    LockerRepository lockerRepository;

    @InjectMocks
    LockerService lockerService;

    @Test
    void 보관함_목록을_조회하면_전체_보관함을_반환한다() {
        // given
        given(lockerRepository.findAll()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE),
                new Locker(2L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED)));

        // when
        List<Locker> result = lockerService.findAllLockers();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1).getUsageStatus()).isEqualTo(UsageStatus.OCCUPIED);
    }

    @Test
    void 존재하는_보관함을_조회하면_보관함_정보를_반환한다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(lockerRepository.findById(1L)).willReturn(Optional.of(locker));

        // when
        Locker result = lockerService.getLocker(1L);

        // then
        assertThat(result.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void 존재하지_않는_보관함을_조회하면_예외가_발생한다() {
        // given
        given(lockerRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lockerService.getLocker(999L))
                .isInstanceOf(LockerNotFoundException.class);
    }

    @Test
    void 보관함_잠금상태를_변경하면_변경된_상태로_반영된다() {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
        given(lockerRepository.findById(1L)).willReturn(Optional.of(locker));

        // when
        Locker result = lockerService.changeLockStatus(1L, LockStatus.UNLOCKED);

        // then
        assertThat(result.getLockStatus()).isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void 존재하지_않는_보관함의_잠금상태를_변경하려_하면_예외가_발생한다() {
        // given
        given(lockerRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lockerService.changeLockStatus(999L, LockStatus.UNLOCKED))
                .isInstanceOf(LockerNotFoundException.class);
    }

    @Test
    void 잠금_조회한_보관함이_없으면_예외가_발생한다() {
        // given
        given(lockerRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lockerService.getLockerForUpdate(999L))
                .isInstanceOf(LockerNotFoundException.class);
    }
}
