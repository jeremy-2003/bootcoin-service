package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionRequest;
import com.bank.bootcoinservice.dto.transaction.BootCoinBankTransactionResponse;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import com.bank.bootcoinservice.service.transaction.BootCoinBankTransactionService;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class BootCoinBankTransactionControllerTest {
    @Mock
    private BootCoinBankTransactionService transactionService;
    @InjectMocks
    private BootCoinBankTransactionController transactionController;
    private BootCoinBankTransactionRequest transactionRequest;
    private BootCoinBankTransactionResponse transactionResponse;
    private String validToken;
    private LocalDateTime now;
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        // Initialize transaction request
        transactionRequest = BootCoinBankTransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .transactionType(TransactionType.P2P)
                .buyerAccountNumber("123456789")
                .build();
        // Initialize transaction response
        transactionResponse = BootCoinBankTransactionResponse.builder()
                .transactionId("transaction123")
                .buyerDocumentNumber("12345678")
                .amount(new BigDecimal("100.00"))
                .totalAmountInPEN(new BigDecimal("380.00"))
                .buyerAccountNumber("123456789")
                .status(TransactionStatus.PENDING)
                .createdAt(now)
                .exchangeRate(new BigDecimal("3.80"))
                .transactionType(TransactionType.P2P)
                .updatedAt(now)
                .build();
        // Initialize valid token
        validToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ" +
                ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
    @Test
    public void requestTransaction_Success() {
        // Given
        when(transactionService.requestTransaction(any(BootCoinBankTransactionRequest.class)))
                .thenReturn(Single.just(transactionResponse));
        // When
        ResponseEntity<BaseResponse<BootCoinBankTransactionResponse>> result =
                transactionController.requestTransaction(transactionRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Transaction request created successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(transactionResponse.getTransactionId(), result.getBody().getData().getTransactionId());
        assertEquals(transactionResponse.getBuyerDocumentNumber(), result.getBody().getData().getBuyerDocumentNumber());
        assertEquals(transactionResponse.getAmount(), result.getBody().getData().getAmount());
        assertEquals(transactionResponse.getTransactionType(), result.getBody().getData().getTransactionType());
        assertEquals(transactionResponse.getStatus(), result.getBody().getData().getStatus());
    }
    @Test
    public void requestTransaction_InvalidToken() {
        // When
        ResponseEntity<BaseResponse<BootCoinBankTransactionResponse>> result =
                transactionController.requestTransaction(transactionRequest, null).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void requestTransaction_InvalidTokenFormat() {
        // When
        ResponseEntity<BaseResponse<BootCoinBankTransactionResponse>> result =
                transactionController.requestTransaction(transactionRequest, "InvalidTokenFormat").blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void requestTransaction_ServiceError() {
        // Given
        when(transactionService.requestTransaction(any(BootCoinBankTransactionRequest.class)))
                .thenReturn(Single.error(new RuntimeException("Service error")));
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            transactionController.requestTransaction(transactionRequest, validToken).blockingGet();
        });
    }
}
