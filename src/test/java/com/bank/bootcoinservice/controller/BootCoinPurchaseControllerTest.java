package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseRequest;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinSellRequest;
import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import com.bank.bootcoinservice.service.bootcoinpurchase.BootCoinPurchaseService;
import io.reactivex.Flowable;
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
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class BootCoinPurchaseControllerTest {
    @Mock
    private BootCoinPurchaseService purchaseService;
    @InjectMocks
    private BootCoinPurchaseController purchaseController;
    private BootCoinPurchaseRequest purchaseRequest;
    private BootCoinPurchaseResponse purchaseResponse;
    private BootCoinSellRequest sellRequest;
    private String validToken;
    private LocalDateTime now;
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        // Initialize purchase request
        purchaseRequest = BootCoinPurchaseRequest.builder()
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.YANKI)
                .build();
        // Initialize purchase response
        purchaseResponse = BootCoinPurchaseResponse.builder()
                .purchaseId("purchase123")
                .buyerDocumentNumber("12345678")
                .buyerPhoneNumber("999888777")
                .paymentMethod(PaymentMethod.YANKI)
                .amount(new BigDecimal("100.00"))
                .totalAmountInPEN(new BigDecimal("380.00"))
                .transactionType(TransactionType.P2P)
                .status(TransactionStatus.PENDING)
                .createdAt(now)
                .build();
        // Initialize sell request
        sellRequest = BootCoinSellRequest.builder()
                .sellerAccountNumber("987654321")
                .build();
        // Initialize valid token
        validToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ" +
                ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
    @Test
    public void requestPurchase_Success() {
        // Given
        when(purchaseService.requestPurchase(any(BootCoinPurchaseRequest.class)))
                .thenReturn(Single.just(purchaseResponse));
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.requestPurchase(purchaseRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Purchase request created successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(purchaseResponse.getPurchaseId(), result.getBody().getData().getPurchaseId());
        assertEquals(purchaseResponse.getBuyerDocumentNumber(), result.getBody().getData().getBuyerDocumentNumber());
        assertEquals(purchaseResponse.getPaymentMethod(), result.getBody().getData().getPaymentMethod());
    }
    @Test
    public void requestPurchase_InvalidToken() {
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.requestPurchase(purchaseRequest, null).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void requestPurchase_InvalidTokenFormat() {
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.requestPurchase(purchaseRequest, "InvalidTokenFormat").blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void requestPurchase_ServiceError() {
        // Given
        when(purchaseService.requestPurchase(any(BootCoinPurchaseRequest.class)))
                .thenReturn(Single.error(new RuntimeException("Service error")));
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseController.requestPurchase(purchaseRequest, validToken).blockingGet();
        });
    }
    @Test
    public void getPendingPurchases_Success() {
        // Given
        BootCoinPurchaseResponse purchase1 = BootCoinPurchaseResponse.builder()
                .purchaseId("purchase1")
                .status(TransactionStatus.PENDING)
                .build();
        BootCoinPurchaseResponse purchase2 = BootCoinPurchaseResponse.builder()
                .purchaseId("purchase2")
                .status(TransactionStatus.WAITING_FOR_SELLER)
                .build();
        when(purchaseService.getPendingPurchases())
                .thenReturn(Flowable.fromIterable(Arrays.asList(purchase1, purchase2)));
        // When
        List<BaseResponse<BootCoinPurchaseResponse>> results =
                purchaseController.getPendingPurchases(validToken).toList().blockingGet();
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(HttpStatus.OK.value(), results.get(0).getStatus());
        assertEquals("Pending purchase requests retrieved successfully", results.get(0).getMessage());
        assertEquals(purchase1.getPurchaseId(), results.get(0).getData().getPurchaseId());
        assertEquals(purchase2.getPurchaseId(), results.get(1).getData().getPurchaseId());
    }
    @Test
    public void getPendingPurchases_InvalidToken() {
        // When
        List<BaseResponse<BootCoinPurchaseResponse>> results =
                purchaseController.getPendingPurchases(null).toList().blockingGet();
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(401, results.get(0).getStatus());
        assertEquals("Missing or invalid token", results.get(0).getMessage());
        assertNull(results.get(0).getData());
    }
    @Test
    public void getPendingPurchases_InvalidTokenFormat() {
        // When
        List<BaseResponse<BootCoinPurchaseResponse>> results =
                purchaseController.getPendingPurchases("InvalidTokenFormat").toList().blockingGet();
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(401, results.get(0).getStatus());
        assertEquals("Missing or invalid token", results.get(0).getMessage());
        assertNull(results.get(0).getData());
    }
    @Test
    public void getPendingPurchases_ServiceError() {
        // Given
        when(purchaseService.getPendingPurchases())
                .thenReturn(Flowable.error(new RuntimeException("Service error")));
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseController.getPendingPurchases(validToken).toList().blockingGet();
        });
    }
    @Test
    public void acceptPurchase_Success() {
        // Given
        BootCoinPurchaseResponse acceptedResponse = BootCoinPurchaseResponse.builder()
                .purchaseId("purchase123")
                .status(TransactionStatus.COMPLETED)
                .sellerAccountNumber("987654321")
                .build();
        when(purchaseService.acceptPurchase(anyString(), any(BootCoinSellRequest.class)))
                .thenReturn(Single.just(acceptedResponse));
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.acceptPurchase("purchase123", sellRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Purchase request accepted successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(acceptedResponse.getPurchaseId(), result.getBody().getData().getPurchaseId());
        assertEquals(TransactionStatus.COMPLETED, result.getBody().getData().getStatus());
        assertEquals("987654321", result.getBody().getData().getSellerAccountNumber());
    }
    @Test
    public void acceptPurchase_InvalidToken() {
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.acceptPurchase("purchase123", sellRequest, null).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void acceptPurchase_InvalidTokenFormat() {
        // When
        ResponseEntity<BaseResponse<BootCoinPurchaseResponse>> result =
                purchaseController.acceptPurchase("purchase123", sellRequest, "InvalidTokenFormat").blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void acceptPurchase_ServiceError() {
        // Given
        when(purchaseService.acceptPurchase(anyString(), any(BootCoinSellRequest.class)))
                .thenReturn(Single.error(new RuntimeException("Service error")));
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseController.acceptPurchase("purchase123", sellRequest, validToken).blockingGet();
        });
    }
}
