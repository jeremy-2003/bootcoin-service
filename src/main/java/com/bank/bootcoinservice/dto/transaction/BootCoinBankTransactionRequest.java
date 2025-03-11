package com.bank.bootcoinservice.dto.transaction;

import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BootCoinBankTransactionRequest {
    @NotBlank
    private String buyerDocumentNumber;
    @NotBlank
    private String buyerAccountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    private PaymentMethod paymentMethod;
    private TransactionType transactionType;
}
