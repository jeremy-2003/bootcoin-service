package com.bank.bootcoinservice.repository;

import com.bank.bootcoinservice.model.transaction.BootCoinTransaction;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;

public interface BootCoinTransactionRepository extends RxJava2CrudRepository<BootCoinTransaction, String> {

}
