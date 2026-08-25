package com.kirin.superservice.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.kirin.superservice.global.auth.LoginCheckInterceptor;
import com.kirin.superservice.global.auth.LoginMemberArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

    @Value("${kirin.image.upload-dir}")
    private String imageUploadDir;

    @Value("${kirin.image.base-url}")
    private String imageBaseUrl;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .order(1)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/members",
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/guest-login",
                        "/api/lockers/**",
                        "/api/actuator/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMemberArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(imageBaseUrl + "/**")
                .addResourceLocations("file:" + imageUploadDir + "/");
    }
}
