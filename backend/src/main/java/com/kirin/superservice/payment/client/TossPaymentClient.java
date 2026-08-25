package com.kirin.superservice.payment.client;

import com.kirin.superservice.payment.exception.PaymentConfirmFailedException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 결제 승인 API를 호출하는 클라이언트.
 * 인증은 토스 시크릿 키를 Basic 인증 방식으로 사용한다.
 */
@Slf4j
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URI = "/v1/payments/confirm";

    private final RestClient restClient;

    public TossPaymentClient(
            @Value("${toss.payment.base-url}") String baseUrl,
            @Value("${toss.payment.secret-key}") String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, encodeBasicAuth(secretKey))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        try {
            return restClient.post()
                    .uri(CONFIRM_URI)
                    .body(new TossConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("토스페이먼츠 결제 승인 API 호출 실패 - orderId={}, status={}, response={}",
                    orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentConfirmFailedException(orderId, e.getResponseBodyAsString());
        }
    }

    private String encodeBasicAuth(String secretKey) {
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
