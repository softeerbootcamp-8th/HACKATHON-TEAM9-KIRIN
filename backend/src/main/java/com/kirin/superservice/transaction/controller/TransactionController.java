package com.kirin.superservice.transaction.controller;

import com.kirin.superservice.global.auth.LoginMember;
import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.dto.request.PurchaseProductForDemoRequest;
import com.kirin.superservice.transaction.dto.request.PurchaseProductRequest;
import com.kirin.superservice.transaction.dto.response.TransactionResponse;
import com.kirin.superservice.transaction.service.PurchaseService;
import com.kirin.superservice.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final PurchaseService purchaseService;
    private final TransactionService transactionService;

    @PostMapping
    public TransactionResponse purchaseProduct(
            @LoginMember Long memberId,
            @RequestBody @Valid PurchaseProductRequest request) {
        Transaction transaction = purchaseService.purchaseProduct(request, memberId);
        return TransactionResponse.fromEntity(transaction);
    }

    /** 데모용 간편결제: 토스 결제위젯 없이 버튼 한 번으로 결제·거래 생성을 끝낸다. */
    @PostMapping("/demo-purchase")
    public TransactionResponse purchaseProductForDemo(
            @LoginMember Long memberId,
            @RequestBody @Valid PurchaseProductForDemoRequest request) {
        Transaction transaction = purchaseService.purchaseProductForDemo(request.productId(), memberId);
        return TransactionResponse.fromEntity(transaction);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(
            @LoginMember Long memberId,
            @PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransaction(transactionId, memberId);
        return TransactionResponse.fromEntity(transaction);
    }

    @PostMapping("/{transactionId}/pickup-complete")
    public TransactionResponse completePickup(
            @LoginMember Long memberId,
            @PathVariable Long transactionId) {
        Transaction transaction = transactionService.completePickup(transactionId, memberId);
        return TransactionResponse.fromEntity(transaction);
    }
}
