package com.kirin.superservice.transaction.controller;

import com.kirin.superservice.transaction.domain.Transaction;
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
    public TransactionResponse purchaseProduct(@RequestBody @Valid PurchaseProductRequest request) {
        Transaction transaction = purchaseService.purchaseProduct(request);
        return TransactionResponse.fromEntity(transaction);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransaction(transactionId);
        return TransactionResponse.fromEntity(transaction);
    }

    @PostMapping("/{transactionId}/pickup-complete")
    public TransactionResponse completePickup(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.completePickup(transactionId);
        return TransactionResponse.fromEntity(transaction);
    }
}
