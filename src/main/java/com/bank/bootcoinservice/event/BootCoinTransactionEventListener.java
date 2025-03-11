package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.bootcoinpurchase.TransactionResponse;
import com.bank.bootcoinservice.service.bootcoinpurchase.BootCoinPurchaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BootCoinTransactionEventListener {
    private final BootCoinPurchaseService bootCoinPurchaseService;

    @Autowired
    public BootCoinTransactionEventListener(BootCoinPurchaseService bootCoinPurchaseService) {
        this.bootCoinPurchaseService = bootCoinPurchaseService;
    }

    @KafkaListener(topics = "bootcoin.transaction.processed", groupId = "bootcoin-service")
    public void updateBootCoinTransaction(TransactionResponse event) {
        log.info("Received Kafka event: {}", event);
        bootCoinPurchaseService.updateTransactionStatus(event)
            .subscribe(
                updatedTransaction -> log.info("Transaction updated successfully: {}", updatedTransaction),
                error -> log.error("Error updating transaction: {}", error.getMessage())
            );
    }
}
