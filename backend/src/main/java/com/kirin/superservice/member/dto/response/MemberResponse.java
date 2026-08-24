package com.kirin.superservice.member.dto.response;

import com.kirin.superservice.member.domain.Member;

public record MemberResponse(Long id, String loginId, String nickname) {

    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(member.getId(), member.getLoginId(), member.getNickname());
    }
}
