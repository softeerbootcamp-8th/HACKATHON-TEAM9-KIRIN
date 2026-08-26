package com.kirin.superservice.member.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.domain.MemberType;
import com.kirin.superservice.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member registerMember(RegisterMemberCommand command) {
        validateDuplicateLoginId(command.loginId());

        Member member = Member.builder()
                .loginId(command.loginId())
                .password(passwordEncoder.encode(command.password()))
                .nickname(command.nickname())
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("회원 가입 완료 - memberId={}", savedMember.getId());
        return savedMember;
    }

    public Member login(LoginCommand command) {
        Member member = memberRepository.findByLoginId(command.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(command.password(), member.getPassword())) {
            log.warn("로그인 비밀번호 불일치 - memberId={}", member.getId());
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        log.info("로그인 성공 - memberId={}", member.getId());
        return member;
    }

    @Transactional
    public Member registerGuest() {
        String token = UUID.randomUUID().toString();
        Member guest = Member.builder()
                .loginId("guest_" + token)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nickname("게스트")
                .memberType(MemberType.GUEST)
                .build();

        Member savedGuest = memberRepository.save(guest);
        savedGuest.assignGuestNickname();
        log.info("게스트 회원 생성 완료 - memberId={}", savedGuest.getId());
        return savedGuest;
    }

    public Member getById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }
}
