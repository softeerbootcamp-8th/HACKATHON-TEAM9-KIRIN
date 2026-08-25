package com.kirin.superservice.transaction.repository;

import com.kirin.superservice.transaction.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
