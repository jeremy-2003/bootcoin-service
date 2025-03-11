package com.bank.bootcoinservice.dto.transaction;

import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BootCoinBankTransactionResponse {
    private String transactionId;
    private String buyerDocumentNumber;
    private BigDecimal amount;
    private BigDecimal totalAmountInPEN;
    private String buyerAccountNumber;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    private BigDecimal exchangeRate;
    private TransactionType transactionType;
    private LocalDateTime updatedAt;
}
