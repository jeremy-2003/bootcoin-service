package com.bank.bootcoinservice.dto.transaction.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BootCoinBankPurchaseCompleted {
    private String transactionId;
    private boolean accepted;
}
