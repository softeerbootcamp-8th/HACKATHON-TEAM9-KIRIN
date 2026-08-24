package com.kirin.superservice.locker.repository;

import com.kirin.superservice.locker.domain.Locker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerRepository extends JpaRepository<Locker, Long> {
}
