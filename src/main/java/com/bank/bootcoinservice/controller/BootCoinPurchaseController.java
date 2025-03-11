package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseRequest;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinSellRequest;
import com.bank.bootcoinservice.service.bootcoinpurchase.BootCoinPurchaseService;
import com.google.common.net.HttpHeaders;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/bootcoin/purchases")
@RequiredArgsConstructor
@Slf4j
public class BootCoinPurchaseController {
    private final BootCoinPurchaseService service;
    @PostMapping
    public Single<ResponseEntity<BaseResponse<BootCoinPurchaseResponse>>> requestPurchase(
            @RequestBody BootCoinPurchaseRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        log.info("Received purchase request: {}", request);
        return service.requestPurchase(request)
                .map(response -> ResponseEntity.ok(
                        BaseResponse.<BootCoinPurchaseResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Purchase request created successfully")
                                .data(response)
                                .build()
                ))
                .doOnError(error -> log.error("Error creating purchase request: {}", error.getMessage()));
    }
    @GetMapping
    public Flowable<BaseResponse<BootCoinPurchaseResponse>> getPendingPurchases(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Flowable.just(new BaseResponse<>(401, "Missing or invalid token", null));
        }
        log.info("Fetching pending purchase requests");
        return service.getPendingPurchases()
                .map(response -> BaseResponse.<BootCoinPurchaseResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Pending purchase requests retrieved successfully")
                        .data(response)
                        .build()
                )
                .doOnError(error -> log.error("Error fetching pending purchases: {}", error.getMessage()));
    }
    @PostMapping("/{purchaseId}/accept")
    public Single<ResponseEntity<BaseResponse<BootCoinPurchaseResponse>>> acceptPurchase(
            @PathVariable String purchaseId,
            @RequestBody BootCoinSellRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        log.info("Accepting purchase request with ID: {}", purchaseId);
        return service.acceptPurchase(purchaseId, request)
                .map(response -> ResponseEntity.ok(
                        BaseResponse.<BootCoinPurchaseResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Purchase request accepted successfully")
                                .data(response)
                                .build()
                ))
                .doOnError(error -> log.error("Error accepting purchase request: {}", error.getMessage()));
    }
}