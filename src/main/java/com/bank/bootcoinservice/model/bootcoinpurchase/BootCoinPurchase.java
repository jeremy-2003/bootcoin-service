package com.bank.bootcoinservice.model.bootcoinpurchase;

import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "bootcoin_purchases")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BootCoinPurchase {
    @Id
    private String id;
    private String buyerDocumentNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private TransactionStatus status;
    private String sellerDocumentNumber; // It is filled when a seller accepts the request
    private String sellerPhoneNumber; // It is filled if the seller uses Yankee
    private String buyerPhoneNumber;
    private String sellerAccountNumber; // Fill in if the seller uses transfer
    private String messageResponse;
    private String buyerAccountNumber;
    private TransactionType transactionType;
    private BigDecimal totalAmountInPEN;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}