package com.kirin.superservice.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kirin.superservice.payment.client.TossConfirmResponse;
import com.kirin.superservice.payment.client.TossPaymentClient;
import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    TossPaymentClient tossPaymentClient;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void 결제_승인을_요청하면_토스_응답을_승인결과로_반환한다() {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("paymentKey", "orderId", 1000L);
        TossConfirmResponse tossResponse = new TossConfirmResponse(
                "paymentKey", "orderId", "DONE", 1000L, "카드", "2026-08-25T00:00:00+09:00");
        given(tossPaymentClient.confirmPayment("paymentKey", "orderId", 1000L)).willReturn(tossResponse);

        // when
        PaymentConfirmResponse result = paymentService.confirmPayment(request);

        // then
        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.amount()).isEqualTo(1000L);
        assertThat(result.paymentKey()).isEqualTo("paymentKey");
    }
}
