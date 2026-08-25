package com.kirin.superservice.payment.controller;

import com.kirin.superservice.payment.dto.request.PaymentConfirmRequest;
import com.kirin.superservice.payment.dto.response.PaymentConfirmResponse;
import com.kirin.superservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public PaymentConfirmResponse confirmPayment(@RequestBody @Valid PaymentConfirmRequest request) {
        return paymentService.confirmPayment(request);
    }
}
