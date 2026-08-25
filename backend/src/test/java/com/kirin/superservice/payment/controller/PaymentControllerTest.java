package com.kirin.superservice.payment.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.payment.exception.PaymentConfirmFailedException;
import com.kirin.superservice.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    SlackErrorNotifier slackErrorNotifier;

    @Test
    void 유효한_결제정보로_승인을_요청하면_200과_승인결과를_반환한다() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "orderId", 1000L);
        PaymentConfirmResponse response = new PaymentConfirmResponse(
                "paymentKey", "orderId", "DONE", 1000L, "2026-08-25T00:00:00+09:00");
        given(paymentService.confirmPayment(request)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("""
                                {"paymentKey":"paymentKey","orderId":"orderId","amount":1000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void 결제_승인에_실패하면_400을_반환한다() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "orderId", 1000L);
        given(paymentService.confirmPayment(request))
                .willThrow(new PaymentConfirmFailedException("orderId", "REJECT_CARD_COMPANY"));

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("""
                                {"paymentKey":"paymentKey","orderId":"orderId","amount":1000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_CONFIRM_FAILED"));
    }

    @Test
    void 필수값이_없으면_400을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("""
                                {"paymentKey":"","orderId":"orderId","amount":1000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
