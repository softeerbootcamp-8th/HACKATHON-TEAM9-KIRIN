package com.kirin.superservice.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.payment.exception.PaymentConfirmFailedException;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.exception.ProductNotSellingException;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.exception.TransactionNotFoundException;
import com.kirin.superservice.transaction.service.PurchaseService;
import com.kirin.superservice.transaction.service.TransactionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PurchaseService purchaseService;

    @MockitoBean
    TransactionService transactionService;

    @MockitoBean
    SlackErrorNotifier slackErrorNotifier;

    private static final String 구매_요청_본문 = """
            {
              "productId": 1,
              "buyerName": "지훈",
              "paymentKey": "payment_key_1",
              "orderId": "order_1",
              "amount": 300000
            }
            """;

    private Transaction 거래(TransactionStatus status) {
        return new Transaction(1L, 1L, 1L, "지훈", 300000L, "payment_key_1", "order_1",
                "2026-08-25T12:00:00+09:00", status, LocalDateTime.now());
    }

    @Test
    void 유효한_구매정보로_구매하면_200과_거래정보를_반환한다() throws Exception {
        // given
        given(purchaseService.purchaseProduct(any(PurchaseProductRequest.class)))
                .willReturn(거래(TransactionStatus.PAID));

        // when & then
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(구매_요청_본문))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void 구매자명이_없으면_400을_반환한다() throws Exception {
        // given
        String 이름_없는_요청 = """
                {
                  "productId": 1,
                  "paymentKey": "payment_key_1",
                  "orderId": "order_1",
                  "amount": 300000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(이름_없는_요청))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 결제_승인에_실패하면_400을_반환한다() throws Exception {
        // given
        given(purchaseService.purchaseProduct(any(PurchaseProductRequest.class)))
                .willThrow(new PaymentConfirmFailedException("order_1", "카드 한도 초과"));

        // when & then
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(구매_요청_본문))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_CONFIRM_FAILED"));
    }

    @Test
    void 판매중이_아닌_물품을_구매하면_409를_반환한다() throws Exception {
        // given
        given(purchaseService.purchaseProduct(any(PurchaseProductRequest.class)))
                .willThrow(new ProductNotSellingException(1L, ProductStatus.SOLD));

        // when & then
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content(구매_요청_본문))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_SELLING"));
    }

    @Test
    void 수령을_확인하면_200과_수령완료_거래를_반환한다() throws Exception {
        // given
        given(transactionService.completePickup(1L)).willReturn(거래(TransactionStatus.DONE));

        // when & then
        mockMvc.perform(post("/api/transactions/1/pickup-complete")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void 존재하지_않는_거래를_조회하면_404를_반환한다() throws Exception {
        // given
        given(transactionService.getTransaction(999L))
                .willThrow(new TransactionNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/api/transactions/999")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }
}
