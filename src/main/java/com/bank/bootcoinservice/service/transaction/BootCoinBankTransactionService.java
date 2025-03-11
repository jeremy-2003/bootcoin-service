package com.bank.bootcoinservice.service.transaction;

import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionRequest;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionResponse;
import com.bank.bootcoinservice.dto.transaction.bank.BootCoinBankPurchaseCompleted;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface BootCoinBankTransactionService {
    Single<BootCoinBankTransactionResponse> requestTransaction(BootCoinBankTransactionRequest request);
    Completable processTransactionResult(BootCoinBankPurchaseCompleted event);
}
