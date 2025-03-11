package com.bank.bootcoinservice.service.bootcoinpurchase;

import com.bank.bootcoinservice.dto.bootcoinpurchase.*;
import com.bank.bootcoinservice.model.bootcoinpurchase.BootCoinPurchase;
import com.bank.bootcoinservice.model.transaction.BootCoinTransaction;
import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import com.bank.bootcoinservice.repository.BootCoinPurchaseRepository;
import com.bank.bootcoinservice.repository.BootCoinTransactionRepository;
import com.bank.bootcoinservice.repository.BootCoinUserRepository;
import com.bank.bootcoinservice.service.exchangerate.BootCoinExchangeRateService;
import io.reactivex.Single;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BootCoinPurchaseServiceImpl implements BootCoinPurchaseService {
    private final BootCoinPurchaseRepository repository;
    private final BootCoinUserRepository bootCoinUserRepository;
    private final BootCoinTransactionRepository bootCoinTransactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BootCoinExchangeRateService exchangeRateService;
    @Override
    public Single<BootCoinPurchaseResponse> requestPurchase(BootCoinPurchaseRequest request) {
        return bootCoinUserRepository.findByDocumentNumber(request.getBuyerDocumentNumber())
                .switchIfEmpty(Maybe.error(new IllegalArgumentException("User not found.")))
                .flatMapSingle(user -> {
                    if (request.getPaymentMethod() == PaymentMethod.YANKI && !user.isHasYanki()) {
                        return Single.error(new IllegalArgumentException("User does not have Yanki enabled."));
                    }
                    if (request.getPaymentMethod() == PaymentMethod.TRANSFER
                        && (user.getBankAccountId() == null
                        || user.getBankAccountId().isEmpty())) {
                        return Single.error(new IllegalArgumentException("User does not have a valid bank account."));
                    }
                    return exchangeRateService.getCurrentExchangeRate()
                            .map(exchangeRate -> request.getAmount().multiply(exchangeRate.getSellRate()))
                            .flatMap(totalAmountInPEN -> {
                                BootCoinPurchase purchase = BootCoinPurchase.builder()
                                        .buyerDocumentNumber(request.getBuyerDocumentNumber())
                                        .paymentMethod(request.getPaymentMethod())
                                        .buyerPhoneNumber(user.getPhoneNumber())
                                        .buyerAccountNumber(user.getBankAccountId())
                                        .amount(request.getAmount())
                                        .totalAmountInPEN(totalAmountInPEN)
                                        .status(TransactionStatus.WAITING_FOR_SELLER)
                                        .transactionType(TransactionType.P2P)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                return repository.save(purchase).map(this::mapToResponse);
                            });
                });
    }
    @Override
    public Flowable<BootCoinPurchaseResponse> getPendingPurchases() {
        return repository.findByStatus(TransactionStatus.WAITING_FOR_SELLER)
                .map(this::mapToResponse);
    }
    @Override
    public Single<BootCoinPurchaseResponse> acceptPurchase(String purchaseId,
                                                           BootCoinSellRequest request) {
        return repository.findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER)
                .switchIfEmpty(Maybe.error(new IllegalArgumentException("Purchase request " +
                    "not found or already taken.")))
                .flatMapSingle(purchase ->
                        bootCoinUserRepository.findByDocumentNumber(request.getSellerDocumentNumber())
                                .switchIfEmpty(Maybe.error(new IllegalArgumentException("Seller not found.")))
                                .flatMapSingle(seller -> {
                                    if (purchase.getPaymentMethod() == PaymentMethod.YANKI
                                        && !seller.isHasYanki()) {
                                        return Single.error(new IllegalArgumentException("Seller does " +
                                            "not have Yanki enabled."));
                                    }
                                    if (purchase.getPaymentMethod() == PaymentMethod.TRANSFER &&
                                        (seller.getBankAccountId() == null
                                        || seller.getBankAccountId().isEmpty())) {
                                        return Single.error(new IllegalArgumentException("Seller does " +
                                            "not have a valid bank account."));
                                    }
                                    if (purchase.getPaymentMethod() == PaymentMethod.YANKI &&
                                            !seller.getPhoneNumber().equals(request.getSellerPhoneNumber())) {
                                        return Single.error(new IllegalArgumentException("Seller phone " +
                                            "number does not match."));
                                    }
                                    if (purchase.getPaymentMethod() == PaymentMethod.TRANSFER &&
                                            !seller.getBankAccountId().equals(request.getSellerAccountNumber())) {
                                        return Single.error(new IllegalArgumentException("Seller bank " +
                                            "account does not match."));
                                    }
                                    if (seller.getBalance().compareTo(purchase.getAmount()) < 0) {
                                        return Single.error(new IllegalArgumentException("Seller does not have " +
                                            "enough balance to take the offer."));
                                    }
                                    purchase.setSellerDocumentNumber(request.getSellerDocumentNumber());
                                    if (purchase.getPaymentMethod() == PaymentMethod.YANKI) {
                                        purchase.setSellerPhoneNumber(request.getSellerPhoneNumber());
                                    } else {
                                        purchase.setSellerAccountNumber(request.getSellerAccountNumber());
                                    }
                                    purchase.setStatus(TransactionStatus.PROCESSING);
                                    purchase.setUpdatedAt(LocalDateTime.now());
                                    return repository.save(purchase)
                                            .doOnSuccess(this::sendTransactionEvent)
                                            .map(this::mapToResponse);
                                })
                );
    }
    private void sendTransactionEvent(BootCoinPurchase purchase) {
        if (purchase.getPaymentMethod() == PaymentMethod.TRANSFER) {
            TransactionEvent event = TransactionEvent.builder()
                    .purchaseId(purchase.getId())
                    .buyerDocumentNumber(purchase.getBuyerDocumentNumber())
                    .sellerDocumentNumber(purchase.getSellerDocumentNumber())
                    .amount(purchase.getAmount())
                    .totalAmountInPEN(purchase.getTotalAmountInPEN())
                    .sellerAccountNumber(purchase.getSellerAccountNumber())
                    .buyerAccountNumber(purchase.getBuyerAccountNumber())
                    .transactionType("P2P")
                    .build();
            kafkaTemplate.send("bootcoin.transaction.transfer.requested", event);
        } else {
            YankiEvent event = YankiEvent.builder()
                    .purchaseId(purchase.getId())
                    .buyerDocumentNumber(purchase.getBuyerDocumentNumber())
                    .sellerDocumentNumber(purchase.getSellerDocumentNumber())
                    .amount(purchase.getAmount())
                    .totalAmountInPEN(purchase.getTotalAmountInPEN())
                    .sellerPhoneNumber(purchase.getSellerPhoneNumber())
                    .buyerPhoneNumber(purchase.getBuyerPhoneNumber())
                    .transactionType("P2P")
                    .build();
            kafkaTemplate.send("bootcoin.transaction.yanki.requested", event);
        }
    }
    @Override
    public Single<BootCoinPurchase> getById(String id) {
        return repository.findById(id).toSingle();
    }
    @Override
    public Single<BootCoinPurchase> updateTransactionStatus(TransactionResponse response) {
        TransactionStatus newStatus = response.isSuccess() ? TransactionStatus.COMPLETED : TransactionStatus.FAILED;
        return repository.findById(response.getTransactionId())
                .switchIfEmpty(Maybe.error(new IllegalArgumentException("Transaction not found.")))
                .flatMapSingle(transaction -> {
                    transaction.setStatus(newStatus);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transaction.setMessageResponse(response.getMessage());
                    return repository.save(transaction)
                            .flatMap(updatedTransaction -> {
                                if (newStatus == TransactionStatus.COMPLETED) {
                                    BootCoinTransaction bootCoinTransaction = BootCoinTransaction.builder()
                                            .id(updatedTransaction.getId())
                                            .paymentMethod(updatedTransaction.getPaymentMethod())
                                            .transactionType(updatedTransaction.getTransactionType())
                                            .status(updatedTransaction.getStatus())
                                            .amount(updatedTransaction.getAmount())
                                            .totalAmountInPEN(updatedTransaction.getTotalAmountInPEN())
                                            .buyerDocumentNumber(updatedTransaction.getBuyerDocumentNumber())
                                            .buyerAccountNumber(updatedTransaction.getBuyerAccountNumber())
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .sellerDocumentNumber(updatedTransaction.getSellerDocumentNumber())
                                            .sellerPhoneNumber(updatedTransaction.getSellerPhoneNumber())
                                            .sellerAccountNumber(updatedTransaction.getSellerAccountNumber())
                                            .buyerPhoneNumber(updatedTransaction.getBuyerPhoneNumber())
                                            .build();
                                    return bootCoinTransactionRepository.save(bootCoinTransaction)
                                            .flatMap(savedTransaction -> updateUserBalance(updatedTransaction));
                                }
                                return Single.just(updatedTransaction);
                            });
                })
                .doOnSuccess(updatedTransaction -> log.info("Transaction successfully updated: {}", updatedTransaction))
                .doOnError(error -> log.error("Failed to update transaction: {}", error.getMessage()));
    }
    private Single<BootCoinPurchase> updateUserBalance(BootCoinPurchase transaction) {
        return bootCoinUserRepository.findByDocumentNumber(transaction.getBuyerDocumentNumber())
                .switchIfEmpty(Maybe.error(new IllegalArgumentException("Buyer not found.")))
                .flatMapSingle(buyer -> {
                    buyer.setBalance(buyer.getBalance().add(transaction.getAmount()));
                    return bootCoinUserRepository.save(buyer);
                })
                .flatMap(buyerUpdated -> bootCoinUserRepository
                    .findByDocumentNumber(transaction.getSellerDocumentNumber())
                        .switchIfEmpty(Maybe.error(new IllegalArgumentException("Seller not found.")))
                        .flatMapSingle(seller -> {
                            seller.setBalance(seller.getBalance().subtract(transaction.getAmount()));
                            return bootCoinUserRepository.save(seller);
                        })
                )
                .map(sellerUpdated -> transaction);
    }
    private BootCoinPurchaseResponse mapToResponse(BootCoinPurchase purchase) {
        return BootCoinPurchaseResponse.builder()
                .purchaseId(purchase.getId())
                .buyerDocumentNumber(purchase.getBuyerDocumentNumber())
                .paymentMethod(purchase.getPaymentMethod())
                .amount(purchase.getAmount())
                .status(purchase.getStatus())
                .sellerDocumentNumber(purchase.getSellerDocumentNumber())
                .sellerPhoneNumber(purchase.getSellerPhoneNumber())
                .sellerAccountNumber(purchase.getSellerAccountNumber())
                .totalAmountInPEN(purchase.getTotalAmountInPEN())
                .transactionType(purchase.getTransactionType())
                .createdAt(purchase.getCreatedAt())
                .build();
    }
}