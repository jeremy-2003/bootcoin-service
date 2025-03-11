package com.bank.bootcoinservice.service.exchangerate;

import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import io.reactivex.Single;
import java.math.BigDecimal;

public interface BootCoinExchangeRateService {
    Single<BootCoinExchangeRate> getCurrentExchangeRate();
    Single<BootCoinExchangeRate> saveExchangeRateToCache(BigDecimal buyRate, BigDecimal sellRate);
    Single<BootCoinExchangeRate> updateExchangeRate(BigDecimal buyRate, BigDecimal sellRate);
}

