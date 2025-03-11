package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountRequest;
import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountResponse;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiRequest;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiResponse;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationRequest;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationResponse;
import com.bank.bootcoinservice.dto.login.LoginRequest;
import com.bank.bootcoinservice.security.JwtProvider;
import com.bank.bootcoinservice.service.user.BootCoinUserService;
import com.google.common.net.HttpHeaders;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bootcoin/user")
@RequiredArgsConstructor
public class BootCoinUserController {
    private final BootCoinUserService bootCoinUserService;
    private final JwtProvider jwtProvider;
    @PostMapping("/register")
    public Single<ResponseEntity<BaseResponse<BootCoinUserRegistrationResponse>>> registerUser(
            @RequestBody BootCoinUserRegistrationRequest request) {
        return bootCoinUserService.registerUser(request)
                .map(response -> ResponseEntity.ok(
                        new BaseResponse<>(HttpStatus.CREATED.value(),
                            "User registered successfully",
                            response)))
                .onErrorReturn(throwable -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        throwable.getMessage(), null)));
    }
    @PostMapping("/login")
    public Single<ResponseEntity<BaseResponse<String>>> login(@RequestBody LoginRequest request) {
        return bootCoinUserService.validateUser(request.getPhoneNumber(), request.getDocumentNumber())
                .map(isValid -> {
                    if (isValid) {
                        String token = jwtProvider.generateToken(request.getPhoneNumber());
                        return ResponseEntity.ok(new BaseResponse<>(200, "Success login", token));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new BaseResponse<>(401, "Invalid credentials", null));
                    }
                });
    }
    @PostMapping("/associate-yanki")
    public Single<ResponseEntity<BaseResponse<AssociateYankiResponse>>> associateYanki(
            @RequestBody AssociateYankiRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        return bootCoinUserService.associateYanki(request)
                .map(response -> ResponseEntity.ok(
                    new BaseResponse<>(HttpStatus.OK.value(),
                        "Yanki associated successfully", response)))
                .onErrorReturn(throwable -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), throwable.getMessage(), null)));
    }

    @PostMapping("/associate-bank-account")
    public Single<ResponseEntity<BaseResponse<AssociateBankAccountResponse>>> associateBankAccount(
            @RequestBody AssociateBankAccountRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Single.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Missing or invalid token", null)));
        }
        return bootCoinUserService.associateBankAccount(request)
                .map(response -> ResponseEntity.ok(
                    new BaseResponse<>(HttpStatus.OK.value(),
                    "Bank account associated successfully", response)))
                .onErrorReturn(throwable -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), throwable.getMessage(), null)));
    }
}
