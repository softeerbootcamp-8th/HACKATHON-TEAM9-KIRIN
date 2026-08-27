package com.kirin.superservice.product.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.dto.request.UpdateProductRequest;
import com.kirin.superservice.product.exception.ProductNotFoundException;
import com.kirin.superservice.product.exception.SellerMismatchException;
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

    @MockitoBean
    SlackErrorNotifier slackErrorNotifier;

    private static final String 등록_요청_본문 = """
            {
              "name": "아이패드",
              "price": 300000,
              "description": "상태 좋고 사용감 거의 없어요"
            }
            """;

    private Product 물품(Long id, ProductStatus status) {
        Product product = new Product(id, status == ProductStatus.PREPARING ? null : 1L,
                "아이패드", 300000L, "상태 좋음", null, 1L, "원기", status, LocalDateTime.now());
        if (status == ProductStatus.RESERVED) {
            product.reserveLocker(1L, LocalDateTime.of(2026, 8, 25, 12, 0),
                    LocalDateTime.of(2026, 8, 25, 16, 0));
        }
        return product;
    }

    @Test
    void 유효한_물품정보로_등록하면_200과_사물함없는_물품정보를_반환한다() throws Exception {
        // given
        given(productService.registerProduct(any(RegisterProductRequest.class), any(Long.class)))
                .willReturn(물품(1L, ProductStatus.PREPARING));

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(등록_요청_본문))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.lockerId").value(nullValue()))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void 여러_장의_사진으로_등록하면_등록한_순서_그대로_반환한다() throws Exception {
        // given
        Product product = new Product(1L, null, "아이패드", 300000L, "상태 좋음",
                List.of("/images/1.jpg", "/images/2.jpg"), 1L, "원기", ProductStatus.PREPARING,
                LocalDateTime.now());
        given(productService.registerProduct(any(RegisterProductRequest.class), any(Long.class)))
                .willReturn(product);
        String 여러장_등록_요청 = """
                {
                  "name": "아이패드",
                  "price": 300000,
                  "description": "상태 좋고 사용감 거의 없어요",
                  "imageUrls": ["/images/1.jpg", "/images/2.jpg"]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(여러장_등록_요청))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls[0]").value("/images/1.jpg"))
                .andExpect(jsonPath("$.imageUrls[1]").value("/images/2.jpg"));
    }

    @Test
    void 로그인하지_않고_물품을_등록하면_401을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(등록_요청_본문))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 물품명이_없으면_400을_반환한다() throws Exception {
        // given
        String 이름_없는_요청 = """
                {
                  "price": 300000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(이름_없는_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 상품_설명이_없으면_400을_반환한다() throws Exception {
        // given
        String 설명_없는_요청 = """
                {
                  "name": "아이패드",
                  "price": 300000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(설명_없는_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 상품_설명이_10자_미만이면_400을_반환한다() throws Exception {
        // given
        String 짧은_설명_요청 = """
                {
                  "name": "아이패드",
                  "price": 300000,
                  "description": "상태 좋음"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(짧은_설명_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 판매_가격이_최소가격보다_낮으면_400을_반환한다() throws Exception {
        // given
        String 너무_낮은_가격_요청 = """
                {
                  "name": "아이패드",
                  "price": 999
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(너무_낮은_가격_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 판매_가격이_최대가격보다_높으면_400을_반환한다() throws Exception {
        // given
        String 너무_높은_가격_요청 = """
                {
                  "name": "아이패드",
                  "price": 1000000001
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(너무_높은_가격_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 존재하는_물품을_조회하면_200과_물품정보를_반환한다() throws Exception {
        // given
        given(productService.getProduct(1L)).willReturn(물품(1L, ProductStatus.SELLING));
        given(productService.countCompletedSales(1L)).willReturn(12L);

        // when & then
        mockMvc.perform(get("/api/products/1")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("아이패드"))
                .andExpect(jsonPath("$.price").value(300000))
                .andExpect(jsonPath("$.sellerCompletedSalesCount").value(12));
    }

    @Test
    void 존재하지_않는_물품을_조회하면_404를_반환한다() throws Exception {
        // given
        given(productService.getProduct(999L)).willThrow(new ProductNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/api/products/999")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void 유효한_정보로_수정하면_200과_수정된_물품정보를_반환한다() throws Exception {
        // given
        Product 수정된_물품 = new Product(1L, null, "수정된 이름", 500000L, "수정된 설명 충분히 길게",
                null, 1L, "원기", ProductStatus.PREPARING, LocalDateTime.now());
        given(productService.updateProduct(any(Long.class), any(UpdateProductRequest.class), any(Long.class)))
                .willReturn(수정된_물품);
        String 수정_요청 = """
                {
                  "name": "수정된 이름",
                  "price": 500000,
                  "description": "수정된 설명 충분히 길게"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(수정_요청))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 이름"))
                .andExpect(jsonPath("$.price").value(500000))
                .andExpect(jsonPath("$.description").value("수정된 설명 충분히 길게"));
    }

    @Test
    void 다른_판매자의_물품을_수정하면_403을_반환한다() throws Exception {
        // given
        given(productService.updateProduct(any(Long.class), any(UpdateProductRequest.class), any(Long.class)))
                .willThrow(new SellerMismatchException(1L));
        String 수정_요청 = """
                {
                  "name": "수정된 이름",
                  "price": 500000,
                  "description": "수정된 설명 충분히 길게"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 2L)
                        .content(수정_요청))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELLER_MISMATCH"));
    }

    @Test
    void 물품_목록을_조회하면_200과_목록을_반환한다() throws Exception {
        // given
        given(productService.findAllProductsByStatus(ProductStatus.SELLING))
                .willReturn(List.of(물품(1L, ProductStatus.SELLING), 물품(2L, ProductStatus.SELLING)));

        // when & then
        mockMvc.perform(get("/api/products")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId").value(1))
                .andExpect(jsonPath("$.products[1].productId").value(2))
                .andExpect(jsonPath("$.products[0].status").value("SELLING"));
    }

    @Test
    void 내_물품_목록을_조회하면_200과_최신순_목록을_반환한다() throws Exception {
        // given
        given(productService.findAllProductsBySellerMemberId(1L, null))
                .willReturn(List.of(물품(2L, ProductStatus.PREPARING), 물품(1L, ProductStatus.SOLD)));

        // when & then
        mockMvc.perform(get("/api/products/me")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId").value(2))
                .andExpect(jsonPath("$.products[1].productId").value(1));
    }

    @Test
    void 내_물품_목록에_상태별_시각_정보가_포함된다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product 예약중_물품 = new Product(1L, 1L, "아이패드", 300000L, "상태 좋음", null, 1L, "원기",
                ProductStatus.RESERVED, now);
        예약중_물품.reserveLocker(1L, now, now.plusHours(4));

        Product 판매중_물품 = new Product(2L, 2L, "맥북", 1000000L, "상태 좋음", null, 1L, "원기",
                ProductStatus.SELLING, now);
        판매중_물품.completeDeposit(now, now.plusDays(7));

        Product 판매완료_물품 = new Product(3L, null, "지갑", 170000L, "상태 좋음", null, 1L, "원기",
                ProductStatus.SOLD, now);
        판매완료_물품.markSold(now);

        given(productService.findAllProductsBySellerMemberId(1L, null))
                .willReturn(List.of(예약중_물품, 판매중_물품, 판매완료_물품));

        // when & then
        mockMvc.perform(get("/api/products/me")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].reservationExpiresAt").exists())
                .andExpect(jsonPath("$.products[0].sellingExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.products[1].sellingStartedAt").exists())
                .andExpect(jsonPath("$.products[1].sellingExpiresAt").exists())
                .andExpect(jsonPath("$.products[2].soldAt").exists());
    }

    @Test
    void 로그인하지_않고_내_물품_목록을_조회하면_401을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 내_물품_목록을_상태로_필터링할_수_있다() throws Exception {
        // given
        given(productService.findAllProductsBySellerMemberId(1L, ProductStatus.RESERVED))
                .willReturn(List.of(물품(1L, ProductStatus.RESERVED)));

        // when & then
        mockMvc.perform(get("/api/products/me")
                        .param("status", "RESERVED")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId").value(1))
                .andExpect(jsonPath("$.products[0].status").value("RESERVED"));
    }

    @Test
    void 사물함을_예약하면_200과_예약된_물품정보를_반환한다() throws Exception {
        // given
        given(productService.reserveLocker(any(Long.class), any(ReserveLockerRequest.class), any(Long.class)))
                .willReturn(물품(1L, ProductStatus.RESERVED));

        // when & then
        mockMvc.perform(post("/api/products/1/locker-reservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("""
                                {
                                  "lockerId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void 다른_회원이_사물함을_예약하면_403을_반환한다() throws Exception {
        // given
        given(productService.reserveLocker(any(Long.class), any(ReserveLockerRequest.class), any(Long.class)))
                .willThrow(new SellerMismatchException(1L));

        // when & then
        mockMvc.perform(post("/api/products/1/locker-reservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 2L)
                        .content("""
                                {
                                  "lockerId": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELLER_MISMATCH"));
    }

    @Test
    void 예약을_취소하면_200과_준비중_물품정보를_반환한다() throws Exception {
        // given
        given(productService.cancelLockerReservation(any(Long.class), any(Long.class)))
                .willReturn(물품(1L, ProductStatus.PREPARING));

        // when & then
        mockMvc.perform(post("/api/products/1/locker-reservation/cancel")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.lockerId").value(nullValue()))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void 물품_투입을_시작하면_200과_예약중_물품정보를_반환한다() throws Exception {
        // given
        Product product = 물품(1L, ProductStatus.RESERVED);
        product.startDeposit(LocalDateTime.of(2026, 8, 25, 12, 0));
        given(productService.startDeposit(any(Long.class), any(Long.class)))
                .willReturn(product);

        // when & then
        mockMvc.perform(post("/api/products/1/deposit-start")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void 물품_투입을_완료하면_200과_판매중_물품정보를_반환한다() throws Exception {
        // given
        Product product = 물품(1L, ProductStatus.RESERVED);
        product.startDeposit(LocalDateTime.of(2026, 8, 25, 11, 59));
        product.completeDeposit(LocalDateTime.of(2026, 8, 25, 12, 0),
                LocalDateTime.of(2026, 9, 1, 12, 0));
        given(productService.completeDeposit(any(Long.class), any(Long.class)))
                .willReturn(product);

        // when & then
        mockMvc.perform(post("/api/products/1/deposit-complete")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("SELLING"));
    }

    @Test
    void 판매자_회수를_시작하면_200과_판매대기_물품정보를_반환한다() throws Exception {
        // given
        given(productService.startRecovery(any(Long.class), any(Long.class)))
                .willReturn(물품(1L, ProductStatus.PREPARING));

        // when & then
        mockMvc.perform(post("/api/products/1/recovery-start")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void 판매자_회수를_완료하면_200과_준비중_물품정보를_반환한다() throws Exception {
        // given
        given(productService.completeRecovery(any(Long.class), any(Long.class)))
                .willReturn(물품(1L, ProductStatus.PREPARING));

        // when & then
        mockMvc.perform(post("/api/products/1/recovery-complete")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }
}
