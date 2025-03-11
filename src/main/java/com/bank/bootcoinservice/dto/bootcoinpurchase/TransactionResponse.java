package com.bank.bootcoinservice.dto.bootcoinpurchase;

import lombok.Data;

@Data
public class TransactionResponse {
    private String transactionId;
    private boolean success;
    private String message;
}
