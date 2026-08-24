package com.kirin.superservice.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kirin.superservice.member.domain.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepository {
}
