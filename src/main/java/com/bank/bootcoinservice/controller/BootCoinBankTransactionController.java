package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionRequest;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionResponse;
import com.bank.bootcoinservice.service.transaction.BootCoinBankTransactionService;
import com.google.common.net.HttpHeaders;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bootcoin/transaction-bank")
@RequiredArgsConstructor
@Slf4j
public class BootCoinBankTransactionController {
    private final BootCoinBankTransactionService transactionService;

    @PostMapping("/request")
    public Single<ResponseEntity<BaseResponse<BootCoinBankTransactionResponse>>> requestTransaction(
            @RequestBody BootCoinBankTransactionRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        log.info("Received transaction request: {}", request);
        return transactionService.requestTransaction(request)
                .map(response -> ResponseEntity.ok(
                        BaseResponse.<BootCoinBankTransactionResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Transaction request created successfully")
                                .data(response)
                                .build()
                ))
                .doOnError(error -> log.error("Error requesting transaction: {}", error.getMessage()));
    }
}
