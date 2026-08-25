package com.kirin.superservice.transaction.repository;

import com.kirin.superservice.transaction.domain.Transaction;
import com.kirin.superservice.transaction.domain.TransactionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByLockerIdAndStatus(Long lockerId, TransactionStatus status);
}
