package com.kirin.superservice.member.service;

public record RegisterMemberCommand(String loginId, String password, String nickname) {
}
