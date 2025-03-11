package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import com.bank.bootcoinservice.service.exchangerate.BootCoinExchangeRateService;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bootcoin/exchange-rate")
@RequiredArgsConstructor
public class BootCoinExchangeRateController {
    private final BootCoinExchangeRateService exchangeRateService;
    @GetMapping
    public Single<ResponseEntity<BaseResponse<BootCoinExchangeRate>>> getExchangeRate() {
        return exchangeRateService.getCurrentExchangeRate()
                .map(rate -> ResponseEntity.ok(
                        BaseResponse.<BootCoinExchangeRate>builder()
                                .status(HttpStatus.OK.value())
                                .message("Exchange rate retrieved")
                                .data(rate)
                                .build()))
                .onErrorReturn(e -> {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(BaseResponse.<BootCoinExchangeRate>builder()
                                    .status(HttpStatus.NOT_FOUND.value())
                                    .message("No exchange rate found")
                                    .data(null)
                                    .build());
                });
    }
    @PostMapping("/create")
    public Single<ResponseEntity<BaseResponse<BootCoinExchangeRate>>> saveExchangeRate(
            @RequestBody BootCoinExchangeRate request) {
        return exchangeRateService.saveExchangeRateToCache(request.getBuyRate(), request.getSellRate())
                .map(rate -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(BaseResponse.<BootCoinExchangeRate>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Exchange rate created")
                                .data(rate)
                                .build()))
                .onErrorReturn(e -> {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(BaseResponse.<BootCoinExchangeRate>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message("Exchange already exists")
                                    .data(null)
                                    .build());
                });
    }
    @PutMapping("/update")
    public Single<ResponseEntity<BaseResponse<BootCoinExchangeRate>>> updateExchangeRate(
            @RequestBody BootCoinExchangeRate request) {
        return exchangeRateService.updateExchangeRate(request.getBuyRate(), request.getSellRate())
                .map(rate -> ResponseEntity.ok(
                        BaseResponse.<BootCoinExchangeRate>builder()
                                .status(HttpStatus.OK.value())
                                .message("Exchange rate updated")
                                .data(rate)
                                .build()))
                .onErrorReturn(e -> {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(BaseResponse.<BootCoinExchangeRate>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message("Exchange rate does not exist, cannot update")
                                    .data(null)
                                    .build());
                });
    }
}