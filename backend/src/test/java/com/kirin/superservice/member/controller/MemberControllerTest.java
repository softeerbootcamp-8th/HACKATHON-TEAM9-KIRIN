package com.kirin.superservice.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.domain.MemberType;
import com.kirin.superservice.member.service.MemberService;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private SlackErrorNotifier slackErrorNotifier;

    @Test
    void 유효한_정보로_회원가입하면_201과_회원정보를_반환한다() {
        // given
        Member member = Member.builder()
                .loginId("loginId")
                .password("encodedPassword")
                .nickname("nickname")
                .build();
        given(memberService.registerMember(any())).willReturn(member);

        // when
        var result = mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"loginId","password":"password1234","nickname":"nickname"}
                        """)
                .exchange();

        // then
        assertThat(result)
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.loginId")
                .isEqualTo("loginId");
    }

    @Test
    void 이미_가입된_아이디로_회원가입하면_409를_반환한다() {
        // given
        given(memberService.registerMember(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        // when
        var result = mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"loginId","password":"password1234","nickname":"nickname"}
                        """)
                .exchange();

        // then
        assertThat(result)
                .hasStatus(409)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("DUPLICATE_LOGIN_ID");
    }

    @Test
    void 로그인_아이디가_비어있으면_400을_반환한다() {
        // when
        var result = mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"","password":"password1234","nickname":"nickname"}
                        """)
                .exchange();

        // then
        assertThat(result).hasStatus(400);
    }

    @Test
    void 로그인한_회원이_내_정보를_조회하면_200과_회원정보를_반환한다() {
        // given
        Member member = Member.builder()
                .loginId("loginId")
                .password("encodedPassword")
                .nickname("nickname")
                .build();
        given(memberService.getById(1L)).willReturn(member);

        // when
        var result = mvc.get().uri("/api/members/me")
                .sessionAttr("loginMemberId", 1L)
                .exchange();

        // then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.nickname")
                .isEqualTo("nickname");
    }

    @Test
    void 게스트_세션으로_내_정보를_조회하면_200을_반환한다() {
        // given
        Member guest = Member.builder()
                .loginId("guest_abc")
                .password("encodedPassword")
                .nickname("게스트-abc12345")
                .memberType(MemberType.GUEST)
                .build();
        given(memberService.getById(1L)).willReturn(guest);

        // when
        var result = mvc.get().uri("/api/members/me")
                .sessionAttr("loginMemberId", 1L)
                .exchange();

        // then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.memberType")
                .isEqualTo("GUEST");
    }

    @Test
    void 로그인하지_않고_내_정보를_조회하면_401을_반환한다() {
        // when
        var result = mvc.get().uri("/api/members/me").exchange();

        // then
        assertThat(result).hasStatus(401);
    }
}
