package com.kirin.superservice.locker.repository;

import com.kirin.superservice.locker.domain.Locker;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Locker l where l.id = :lockerId")
    Optional<Locker> findByIdForUpdate(@Param("lockerId") Long lockerId);
}
