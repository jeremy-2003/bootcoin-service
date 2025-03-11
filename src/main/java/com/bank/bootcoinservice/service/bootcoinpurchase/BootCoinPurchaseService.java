package com.bank.bootcoinservice.service.bootcoinpurchase;

import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseRequest;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinSellRequest;
import com.bank.bootcoinservice.dto.bootcoinpurchase.TransactionResponse;
import com.bank.bootcoinservice.model.bootcoinpurchase.BootCoinPurchase;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface BootCoinPurchaseService {
    Single<BootCoinPurchaseResponse> requestPurchase(BootCoinPurchaseRequest request);
    Flowable<BootCoinPurchaseResponse> getPendingPurchases();
    Single<BootCoinPurchaseResponse> acceptPurchase(String purchaseId, BootCoinSellRequest request);
    Single<BootCoinPurchase> updateTransactionStatus(TransactionResponse response);
    Single<BootCoinPurchase> getById(String id);
}