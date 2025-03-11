package com.bank.bootcoinservice.model.transaction;

public enum TransactionStatus {
    PENDING,
    WAITING_FOR_SELLER,
    PROCESSING,
    COMPLETED,
    FAILED
}
