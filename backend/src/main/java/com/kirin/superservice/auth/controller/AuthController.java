package com.kirin.superservice.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirin.superservice.auth.dto.request.LoginRequest;
import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.dto.response.MemberResponse;
import com.kirin.superservice.member.service.MemberService;
import com.kirin.superservice.product.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final ProductService productService;

    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        Member member = memberService.login(request.toCommand());

        HttpSession session = httpRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        log.info("세션 발급 완료 - memberId={}, sessionId={}", member.getId(), session.getId());

        return ResponseEntity.ok(MemberResponse.fromEntity(member));
    }

    @PostMapping("/guest-login")
    public ResponseEntity<MemberResponse> guestLogin(HttpServletRequest httpRequest) {
        Member member = memberService.registerGuest();
        productService.registerDummyProducts(member);

        HttpSession session = httpRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        log.info("게스트 세션 발급 완료 - memberId={}, sessionId={}", member.getId(), session.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.fromEntity(member));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            log.info("세션 무효화 - sessionId={}", session.getId());
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
