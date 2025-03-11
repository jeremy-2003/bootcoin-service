package com.bank.bootcoinservice.service.exchangerate;

import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import io.reactivex.Single;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BootCoinExchangeRateServiceImpl implements BootCoinExchangeRateService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String EXCHANGE_RATE_KEY = "bootcoin:exchange_rate";
    @Override
    public Single<BootCoinExchangeRate> getCurrentExchangeRate() {
        log.info("Attempting to retrieve exchange rate from Redis with key: {}", EXCHANGE_RATE_KEY);
        return Single.create(emitter ->
                redisTemplate.opsForValue().get(EXCHANGE_RATE_KEY)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSubscribe(s -> log.info("Subscribe to get exchange rate from Redis"))
                        .doOnNext(value -> {
                            if (value == null) {
                                log.warn("Null value retrieved from Redis for exchange rate");
                            } else {
                                log.info("Retrieved exchange rate from cache: value length={}", value.length());
                            }
                        })
                        .flatMap(json -> {
                            try {
                                BootCoinExchangeRate exchangeRate =
                                    objectMapper.readValue(json, BootCoinExchangeRate.class);
                                log.info("Successfully deserialized exchange rate: buy={}, sell={}",
                                        exchangeRate.getBuyRate(), exchangeRate.getSellRate());
                                return Mono.just(exchangeRate);
                            } catch (Exception e) {
                                log.error("Error deserializing exchange rate JSON: {}", e.getMessage(), e);
                                return Mono.error(new RuntimeException("Error parsing exchange rate data", e));
                            }
                        })
                        .switchIfEmpty(Mono.error(new RuntimeException("Exchange rate not found")))
                        .timeout(Duration.ofSeconds(5))
                        .doOnError(TimeoutException.class, e ->
                                log.error("Redis operation timed out for exchange rate"))
                        .doOnError(e -> {
                            if (!(e instanceof TimeoutException)) {
                                log.error("Error retrieving exchange rate from cache: {}", e.getMessage());
                            }
                        })
                        .subscribe(emitter::onSuccess, emitter::onError)
        );
    }
    @Override
    public Single<BootCoinExchangeRate> saveExchangeRateToCache(BigDecimal buyRate, BigDecimal sellRate) {
        if (buyRate == null || sellRate == null) {
            return Single.error(new IllegalArgumentException("Buy rate and sell rate cannot be null"));
        }
        log.info("Attempting to save new exchange rate: buy={}, sell={}", buyRate, sellRate);
        BootCoinExchangeRate exchangeRate = new BootCoinExchangeRate();
        exchangeRate.setBuyRate(buyRate);
        exchangeRate.setSellRate(sellRate);
        exchangeRate.setUpdatedAt(LocalDateTime.now());
        return Single.create(emitter ->
                redisTemplate.opsForValue().get(EXCHANGE_RATE_KEY)
                        .flatMap(json -> {
                            if (json != null) {
                                log.warn("Exchange rate already exists in Redis");
                                return Mono.error(new RuntimeException("Exchange rate already" +
                                    " exists, use update instead"));
                            }
                            return Mono.empty();
                        })
                        .switchIfEmpty(Mono.defer(() ->
                                Mono.fromCallable(() -> objectMapper.writeValueAsString(exchangeRate))
                                    .doOnNext(json -> log.info("Serialized exchange " +
                                        "rate JSON: {}", json))
                                    .doOnError(error -> log.error("Error serializing " +
                                        "exchange rate: {}", error.getMessage()))
                                    .flatMap(exchangeRateJson -> redisTemplate.opsForValue()
                                        .set(EXCHANGE_RATE_KEY, exchangeRateJson)
                                            .doOnNext(result -> log.info("Redis SET result {}", result))
                                            .doOnSuccess(result -> log.info("Successfully " +
                                                "cached exchange rate"))
                                            .doOnError(error -> log.error("Error storing " +
                                                "exchange rate in Redis: {}", error.getMessage()))
                                            .thenReturn(exchangeRate))
                        ))
                        .timeout(Duration.ofSeconds(5))
                        .doOnError(TimeoutException.class, e ->
                                log.error("Redis operation timed out when saving exchange rate"))
                        .doOnError(e -> {
                            if (!(e instanceof TimeoutException)) {
                                log.error("Error saving exchange rate to cache: {}", e.getMessage());
                            }
                        })
                        .subscribe(
                                result -> emitter.onSuccess(exchangeRate),
                                emitter::onError
                        )
        );
    }
    @Override
    public Single<BootCoinExchangeRate> updateExchangeRate(BigDecimal buyRate, BigDecimal sellRate) {
        if (buyRate == null || sellRate == null) {
            return Single.error(new IllegalArgumentException("Buy rate and sell rate cannot be null"));
        }
        log.info("Attempting to update exchange rate: buy={}, sell={}", buyRate, sellRate);
        return Single.create(emitter ->
                redisTemplate.opsForValue().get(EXCHANGE_RATE_KEY)
                        .switchIfEmpty(Mono.error(new RuntimeException("Exchange rate does not exist, cannot update")))
                        .flatMap(json -> {
                            try {
                                BootCoinExchangeRate exchangeRate =
                                    objectMapper.readValue(json, BootCoinExchangeRate.class);
                                log.info("Retrieved existing exchange rate for update: buy={}, sell={}",
                                        exchangeRate.getBuyRate(), exchangeRate.getSellRate());
                                exchangeRate.setBuyRate(buyRate);
                                exchangeRate.setSellRate(sellRate);
                                exchangeRate.setUpdatedAt(LocalDateTime.now());
                                return Mono.fromCallable(() -> objectMapper.writeValueAsString(exchangeRate))
                                    .doOnNext(updatedJson -> log.info("Serialized updated " +
                                        "exchange rate JSON: {}", updatedJson))
                                    .doOnError(error -> log.error("Error serializing " +
                                        "updated exchange rate: {}", error.getMessage()))
                                    .flatMap(exchangeRateJson ->
                                        redisTemplate.opsForValue().set(EXCHANGE_RATE_KEY, exchangeRateJson)
                                            .doOnNext(result ->
                                                log.info("Redis SET result for update {}", result))
                                            .doOnSuccess(result ->
                                                log.info("Successfully updated exchange rate"))
                                            .doOnError(error ->
                                                log.error("Error updating exchange rate in " +
                                                    "Redis: {}", error.getMessage()))
                                            .thenReturn(exchangeRate));
                            } catch (Exception e) {
                                log.error("Error deserializing existing exchange rate JSON: {}", e.getMessage(), e);
                                return Mono.error(new RuntimeException("Error parsing existing exchange rate data", e));
                            }
                        })
                        .timeout(Duration.ofSeconds(5))
                        .doOnError(TimeoutException.class, e ->
                                log.error("Redis operation timed out when updating exchange rate"))
                        .doOnError(e -> {
                            if (!(e instanceof TimeoutException)) {
                                log.error("Error updating exchange rate in cache: {}", e.getMessage());
                            }
                        })
                        .subscribe(emitter::onSuccess, emitter::onError)
        );
    }
}