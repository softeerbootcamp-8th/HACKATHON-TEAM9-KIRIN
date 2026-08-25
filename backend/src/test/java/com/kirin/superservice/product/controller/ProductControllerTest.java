package com.kirin.superservice.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.locker.exception.LockerAlreadyOccupiedException;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    private static final String 등록_요청_본문 = """
            {
              "lockerId": 1,
              "name": "아이패드",
              "price": 300000,
              "description": "상태 좋음",
              "sellerName": "원기"
            }
            """;

    private Product 물품(Long id, ProductStatus status) {
        return new Product(id, 1L, "아이패드", 300000L, "상태 좋음", null, "원기",
                status, LocalDateTime.now());
    }

    @Test
    void 유효한_물품정보로_등록하면_200과_물품정보를_반환한다() throws Exception {
        // given
        given(productService.registerProduct(any(RegisterProductRequest.class)))
                .willReturn(물품(1L, ProductStatus.PREPARING));

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(등록_요청_본문))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void 물품명이_없으면_400을_반환한다() throws Exception {
        // given
        String 이름_없는_요청 = """
                {
                  "lockerId": 1,
                  "price": 300000,
                  "sellerName": "원기"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(이름_없는_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 이미_사용중인_보관함에_등록하면_409를_반환한다() throws Exception {
        // given
        given(productService.registerProduct(any(RegisterProductRequest.class)))
                .willThrow(new LockerAlreadyOccupiedException(1L));

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(등록_요청_본문))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOCKER_ALREADY_OCCUPIED"));
    }

    @Test
    void 존재하는_물품을_조회하면_200과_물품정보를_반환한다() throws Exception {
        // given
        given(productService.getProduct(1L)).willReturn(물품(1L, ProductStatus.SELLING));

        // when & then
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("아이패드"))
                .andExpect(jsonPath("$.price").value(300000));
    }

    @Test
    void 존재하지_않는_물품을_조회하면_404를_반환한다() throws Exception {
        // given
        given(productService.getProduct(999L)).willThrow(new ProductNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void 물품_목록을_조회하면_200과_목록을_반환한다() throws Exception {
        // given
        given(productService.findAllProductsByStatus(ProductStatus.SELLING))
                .willReturn(List.of(물품(1L, ProductStatus.SELLING), 물품(2L, ProductStatus.SELLING)));

        // when & then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId").value(1))
                .andExpect(jsonPath("$.products[1].productId").value(2))
                .andExpect(jsonPath("$.products[0].status").value("SELLING"));
    }

    @Test
    void 등록완료를_요청하면_200과_판매중_물품을_반환한다() throws Exception {
        // given
        given(productService.completeRegistration(1L)).willReturn(물품(1L, ProductStatus.SELLING));

        // when & then
        mockMvc.perform(post("/api/products/1/registration-complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("SELLING"));
    }
}
