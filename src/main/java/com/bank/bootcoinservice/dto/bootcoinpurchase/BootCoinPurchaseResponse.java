package com.bank.bootcoinservice.dto.bootcoinpurchase;

import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BootCoinPurchaseResponse {
    private String purchaseId;
    private String buyerDocumentNumber;
    private String buyerPhoneNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private BigDecimal totalAmountInPEN;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String sellerDocumentNumber;
    private String sellerPhoneNumber;
    private String sellerAccountNumber;
    private LocalDateTime createdAt;
}
