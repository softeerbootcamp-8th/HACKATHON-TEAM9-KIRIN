package com.kirin.superservice.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirin.superservice.global.auth.LoginMember;
import com.kirin.superservice.member.domain.Member;
import com.kirin.superservice.member.dto.request.SignUpRequest;
import com.kirin.superservice.member.dto.response.MemberResponse;
import com.kirin.superservice.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Member member = memberService.registerMember(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.fromEntity(member));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@LoginMember Long memberId) {
        Member member = memberService.getById(memberId);
        return ResponseEntity.ok(MemberResponse.fromEntity(member));
    }
}
