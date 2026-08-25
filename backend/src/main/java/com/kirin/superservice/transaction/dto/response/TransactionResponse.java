package com.kirin.superservice.transaction.dto.response;

import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        Long productId,
        Long lockerId,
        String buyerName,
        Long price,
        TransactionStatus status,
        String orderId,
        String approvedAt,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getProductId(),
                transaction.getLockerId(),
                transaction.getBuyerName(),
                transaction.getPrice(),
                transaction.getStatus(),
                transaction.getOrderId(),
                transaction.getApprovedAt(),
                transaction.getCreatedAt()
        );
    }
}
