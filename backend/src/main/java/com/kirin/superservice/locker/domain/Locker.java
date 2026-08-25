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

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_status", nullable = false, length = 20)
    private UsageStatus usageStatus;

    public Locker(Long id, LockStatus lockStatus, UsageStatus usageStatus) {
        this.id = id;
        this.lockStatus = lockStatus;
        this.usageStatus = usageStatus;
    }

    public void changeLockStatus(LockStatus lockStatus) {
        this.lockStatus = lockStatus;
    }

    public void occupy() {
        this.usageStatus = UsageStatus.OCCUPIED;
    }

    public void reserve() {
        this.usageStatus = UsageStatus.RESERVED;
    }

    public void release() {
        this.usageStatus = UsageStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return this.usageStatus == UsageStatus.AVAILABLE;
    }

    public boolean isReserved() {
        return this.usageStatus == UsageStatus.RESERVED;
    }
}
