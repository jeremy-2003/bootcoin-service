package com.bank.bootcoinservice.service.transaction;

import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionRequest;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionResponse;
import com.bank.bootcoinservice.dto.transaction.bank.BootCoinBankPurchaseCompleted;
import com.bank.bootcoinservice.dto.transaction.bank.BootCoinBankPurchaseRequested;
import com.bank.bootcoinservice.model.transaction.BootCoinTransaction;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.repository.BootCoinTransactionRepository;
import com.bank.bootcoinservice.repository.BootCoinUserRepository;
import com.bank.bootcoinservice.service.exchangerate.BootCoinExchangeRateService;
import io.reactivex.Completable;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class BootCoinBankTransactionServiceImpl implements BootCoinBankTransactionService {
    private final BootCoinTransactionRepository repository;
    private final BootCoinUserRepository bootCoinUserRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BootCoinExchangeRateService exchangeRateService;
    @Override
    public Single<BootCoinBankTransactionResponse> requestTransaction(BootCoinBankTransactionRequest request) {
        log.info("Processing transaction request: {}", request);
        return bootCoinUserRepository.findByDocumentNumber(request.getBuyerDocumentNumber())
                .switchIfEmpty(Single.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    log.info("Found user: {}", user);
                    boolean accountsMatch = user.getBankAccountId() != null &&
                            user.getBankAccountId().equals(request.getBuyerAccountNumber());
                    log.info("User bank account: {}, Request account: {}, Match: {}",
                            user.getBankAccountId(), request.getBuyerAccountNumber(), accountsMatch);
                    if (!accountsMatch) {
                        log.warn("Account mismatch detected. User cannot use this account.");
                        return Single.error(new RuntimeException(
                                "Transaction rejected: The bank account " +
                                    "provided doesn't match your registered account."));
                    }
                    return exchangeRateService.getCurrentExchangeRate()
                            .flatMap(exchangeRate -> {
                                BigDecimal totalAmountInPEN = request.getAmount().multiply(exchangeRate.getBuyRate());
                                BootCoinTransaction transaction = BootCoinTransaction.builder()
                                        .buyerDocumentNumber(request.getBuyerDocumentNumber())
                                        .amount(request.getAmount())
                                        .totalAmountInPEN(totalAmountInPEN)
                                        .buyerAccountNumber(request.getBuyerAccountNumber())
                                        .status(TransactionStatus.PENDING)
                                        .transactionType(request.getTransactionType())
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                log.info("Saving transaction: {}", transaction);
                                return repository.save(transaction)
                                        .map(savedTransaction -> {
                                            BootCoinBankPurchaseRequested event =
                                                BootCoinBankPurchaseRequested.builder()
                                                    .transactionId(savedTransaction.getId())
                                                    .buyerDocumentNumber(savedTransaction.getBuyerDocumentNumber())
                                                    .amount(savedTransaction.getAmount())
                                                    .totalAmountInPEN(savedTransaction.getTotalAmountInPEN())
                                                    .buyerAccountNumber(savedTransaction.getBuyerAccountNumber())
                                                    .build();
                                            log.info("Sending Kafka event: {}", event);
                                            kafkaTemplate.send("bootcoin.bank.purchase.requested", event);
                                            return mapToResponse(savedTransaction);
                                        });
                            });
                });
    }
    @Override
    public Completable processTransactionResult(BootCoinBankPurchaseCompleted event) {
        return repository.findById(event.getTransactionId())
                .flatMapCompletable(transaction -> {
                    transaction.setStatus(event.isAccepted() ? TransactionStatus.COMPLETED : TransactionStatus.FAILED);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    if (event.isAccepted()) {
                        return bootCoinUserRepository.findByDocumentNumber(transaction.getBuyerDocumentNumber())
                                .flatMapCompletable(user -> {
                                    BigDecimal newBalance = user.getBalance().add(transaction.getAmount());
                                    user.setBalance(newBalance);
                                    return bootCoinUserRepository.save(user)
                                            .flatMapCompletable(savedUser ->
                                                    repository.save(transaction).ignoreElement()
                                            );
                                });
                    } else {
                        return repository.save(transaction).ignoreElement();
                    }
                });
    }
    private BootCoinBankTransactionResponse mapToResponse(BootCoinTransaction transaction) {
        return BootCoinBankTransactionResponse.builder()
                .transactionId(transaction.getId())
                .buyerDocumentNumber(transaction.getBuyerDocumentNumber())
                .amount(transaction.getAmount())
                .totalAmountInPEN(transaction.getTotalAmountInPEN())
                .buyerAccountNumber(transaction.getBuyerAccountNumber())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}