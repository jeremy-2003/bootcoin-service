package com.bank.bootcoinservice.repository;

import com.bank.bootcoinservice.model.bootcoinpurchase.BootCoinPurchase;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;

public interface BootCoinPurchaseRepository extends RxJava2CrudRepository<BootCoinPurchase, String> {
    Flowable<BootCoinPurchase> findByStatus(TransactionStatus status);
    Maybe<BootCoinPurchase> findByIdAndStatus(String id, TransactionStatus status);
}
