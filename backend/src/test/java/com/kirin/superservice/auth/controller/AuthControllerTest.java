package com.kirin.superservice.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.service.MemberService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private SlackErrorNotifier slackErrorNotifier;

    @Test
    void 올바른_정보로_로그인하면_200과_세션이_발급된다() {
        // given
        Member member = Member.builder()
                .loginId("loginId")
                .password("encodedPassword")
                .nickname("nickname")
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberService.login(any())).willReturn(member);

        // when
        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"loginId","password":"password1234"}
                        """)
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result.getRequest().getSession(false)).isNotNull();
        assertThat(result.getRequest().getSession(false).getAttribute("loginMemberId")).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_아이디로_로그인하면_404를_반환한다() {
        // given
        given(memberService.login(any())).willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // when
        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"notExist","password":"password1234"}
                        """)
                .exchange();

        // then
        assertThat(result)
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MEMBER_NOT_FOUND");
    }

    @Test
    void 비밀번호가_틀리면_401을_반환한다() {
        // given
        given(memberService.login(any())).willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD));

        // when
        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"loginId","password":"wrongPassword"}
                        """)
                .exchange();

        // then
        assertThat(result).hasStatus(401);
    }

    @Test
    void 로그인한_상태에서_로그아웃하면_세션이_무효화되고_204를_반환한다() {
        // given
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMemberId", 1L);

        // when
        var result = mvc.post().uri("/api/auth/logout")
                .session(session)
                .exchange();

        // then
        assertThat(result).hasStatus(204);
        assertThat(session.isInvalid()).isTrue();
    }
}
