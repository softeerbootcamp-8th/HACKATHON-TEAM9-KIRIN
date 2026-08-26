package com.kirin.superservice.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberType memberType;

    @Builder
    public Member(String loginId, String password, String nickname, MemberType memberType) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.memberType = (memberType != null) ? memberType : MemberType.REGISTERED;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 발급된 회원 ID로 게스트 표시 이름을 "게스트 {ID % 100}" 형태로 확정한다.
     * ID는 저장 후에만 확정되므로, 저장 직후에 호출해야 한다.
     */
    public void assignGuestNickname() {
        this.nickname = "게스트 " + (this.id % 100);
    }
}
