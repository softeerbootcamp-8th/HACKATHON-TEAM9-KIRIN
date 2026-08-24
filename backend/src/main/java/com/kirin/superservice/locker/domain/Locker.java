package com.kirin.superservice.locker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "locker")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locker {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status", nullable = false, length = 20)
    private LockStatus lockStatus;

    public Locker(Long id, LockStatus lockStatus) {
        this.id = id;
        this.lockStatus = lockStatus;
    }

    public void changeLockStatus(LockStatus lockStatus) {
        this.lockStatus = lockStatus;
    }
}
