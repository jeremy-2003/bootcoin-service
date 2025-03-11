package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.transaction.bank.BootCoinBankPurchaseCompleted;
import com.bank.bootcoinservice.service.transaction.BootCoinBankTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BootCoinBankTransactionEventListener {
    private final BootCoinBankTransactionService bootCoinBankTransactionService;
    @Autowired
    public BootCoinBankTransactionEventListener(BootCoinBankTransactionService bootCoinBankTransactionService) {
        this.bootCoinBankTransactionService = bootCoinBankTransactionService;
    }
    @KafkaListener(topics = "bootcoin.bank.purchase.procesed", groupId = "bootcoin-service")
    public void updateTransactionBootCoinBank(BootCoinBankPurchaseCompleted event) {
        log.info("Received Kafka event: {}", event);
        bootCoinBankTransactionService.processTransactionResult(event)
                .doOnComplete(() -> log.info("Transaction processed successfully"))
                .doOnError(error -> log.error("Error processing transaction: {}", error.getMessage(), error))
                .onErrorComplete()
                .subscribe();
    }
}
