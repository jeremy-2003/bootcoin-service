package com.bank.bootcoinservice.dto.bootcoinpurchase;

import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Builder
@Data
public class BootCoinPurchaseRequest {
    private String buyerDocumentNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
}