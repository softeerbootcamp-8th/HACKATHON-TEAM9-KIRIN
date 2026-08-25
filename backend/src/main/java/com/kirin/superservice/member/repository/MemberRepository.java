package com.kirin.superservice.member.repository;

import java.util.Optional;

import com.kirin.superservice.member.domain.Member;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
