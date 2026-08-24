package com.kirin.superservice.global.auth;

import org.springframework.web.servlet.HandlerInterceptor;

import com.kirin.superservice.global.exception.BusinessException;
import com.kirin.superservice.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER_ID) == null) {
            log.warn("인증되지 않은 접근 - uri={}", request.getRequestURI());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return true;
    }
}
