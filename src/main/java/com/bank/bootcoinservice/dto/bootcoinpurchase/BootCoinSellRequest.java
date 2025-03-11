package com.bank.bootcoinservice.dto.bootcoinpurchase;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BootCoinSellRequest {
    private String sellerDocumentNumber;
    private String sellerPhoneNumber; // Only if the payment is Yankee
    private String sellerAccountNumber; // Only if payment is by transfer
}
