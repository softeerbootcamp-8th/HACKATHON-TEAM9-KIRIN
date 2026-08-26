package com.kirin.superservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.domain.MemberType;
import com.kirin.superservice.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 유효한_회원정보로_가입하면_회원이_저장된다() {
        // given
        RegisterMemberCommand command = new RegisterMemberCommand("loginId", "password", "nickname");
        given(memberRepository.existsByLoginId("loginId")).willReturn(false);
        given(passwordEncoder.encode("password")).willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member savedMember = memberService.registerMember(command);

        // then
        assertThat(savedMember.getLoginId()).isEqualTo("loginId");
        assertThat(savedMember.getNickname()).isEqualTo("nickname");
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    void 이미_가입된_아이디로_가입하면_예외가_발생한다() {
        // given
        RegisterMemberCommand command = new RegisterMemberCommand("loginId", "password", "nickname");
        given(memberRepository.existsByLoginId("loginId")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
    }

    @Test
    void 올바른_아이디와_비밀번호로_로그인하면_회원정보를_반환한다() {
        // given
        Member member = Member.builder()
                .loginId("loginId")
                .password("encodedPassword")
                .nickname("nickname")
                .build();
        LoginCommand command = new LoginCommand("loginId", "password");
        given(memberRepository.findByLoginId("loginId")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);

        // when
        Member loginMember = memberService.login(command);

        // then
        assertThat(loginMember.getLoginId()).isEqualTo("loginId");
    }

    @Test
    void 비밀번호가_일치하지_않으면_로그인시_예외가_발생한다() {
        // given
        Member member = Member.builder()
                .loginId("loginId")
                .password("encodedPassword")
                .nickname("nickname")
                .build();
        LoginCommand command = new LoginCommand("loginId", "wrongPassword");
        given(memberRepository.findByLoginId("loginId")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.login(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void 존재하지_않는_아이디로_로그인하면_예외가_발생한다() {
        // given
        LoginCommand command = new LoginCommand("notExist", "password");
        given(memberRepository.findByLoginId("notExist")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.login(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 게스트_로그인하면_GUEST_타입의_회원이_생성된다() {
        // given
        given(passwordEncoder.encode(any())).willReturn("encodedRandomPassword");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 142L);
            return saved;
        });

        // when
        Member guest = memberService.registerGuest();

        // then
        assertThat(guest.getMemberType()).isEqualTo(MemberType.GUEST);
        assertThat(guest.getLoginId()).startsWith("guest_");
        assertThat(guest.getNickname()).isEqualTo("게스트 42");
    }

    @Test
    void 존재하지_않는_회원을_조회하면_예외가_발생한다() {
        // given
        Long notExistId = 999L;
        given(memberRepository.findById(notExistId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getById(notExistId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
