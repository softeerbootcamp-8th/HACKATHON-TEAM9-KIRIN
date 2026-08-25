package com.kirin.superservice.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {

    /** false로 지정하면 로그인하지 않은 요청도 허용하고, 이때 회원 ID로 null을 전달한다. */
    boolean required() default true;
}
