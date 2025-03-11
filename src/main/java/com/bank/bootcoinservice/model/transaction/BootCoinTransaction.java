package com.bank.bootcoinservice.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "bootcoin_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BootCoinTransaction {
    @Id
    private String id;

    private PaymentMethod paymentMethod; //For bank is only TRANSFER
    private TransactionType transactionType; // BANK o P2P
    private TransactionStatus status;
    private BigDecimal amount; //Quantity of bootcoins
    private BigDecimal totalAmountInPEN;

    //For sale to bank
    private String buyerDocumentNumber;
    private String buyerAccountNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //For sale to P2P
    private String sellerDocumentNumber;
    private String sellerPhoneNumber;
    private String sellerAccountNumber;
    private String buyerPhoneNumber;
}

