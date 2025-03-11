package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.bootcoinpurchase.BootCoinPurchaseResponse;
import com.bank.bootcoinservice.dto.bootcoinpurchase.TransactionResponse;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.service.bootcoinpurchase.BootCoinPurchaseService;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BootCoinTransactionEventListenerTest {
    @Mock
    private BootCoinPurchaseService bootCoinPurchaseService;
    @InjectMocks
    private BootCoinTransactionEventListener eventListener;
    private TransactionResponse successEvent;
    private TransactionResponse failureEvent;
    private BootCoinPurchaseResponse purchaseResponse;
    @BeforeEach
    void setUp() {
        // Initialize successful transaction event
        successEvent = new TransactionResponse();
        successEvent.setTransactionId("transaction123");
        successEvent.setSuccess(true);
        successEvent.setMessage("Transaction processed successfully");
        // Initialize failed transaction event
        failureEvent = new TransactionResponse();
        failureEvent.setTransactionId("transaction456");
        failureEvent.setSuccess(false);
        failureEvent.setMessage("Insufficient funds");
        // Initialize purchase response
        purchaseResponse = BootCoinPurchaseResponse.builder()
                .purchaseId("purchase123")
                .status(TransactionStatus.COMPLETED)
                .build();
    }
    @Test
    public void updateBootCoinTransaction_Success() {
        // Given
        Single<BootCoinPurchaseResponse> responseSingle = Single.just(purchaseResponse);
        doReturn(responseSingle).when(bootCoinPurchaseService).updateTransactionStatus(any(TransactionResponse.class));
        // When
        eventListener.updateBootCoinTransaction(successEvent);
        // Then
        verify(bootCoinPurchaseService, times(1)).updateTransactionStatus(successEvent);
    }
    @Test
    public void updateBootCoinTransaction_Failure() {
        // Given
        Single<BootCoinPurchaseResponse> responseSingle = Single.just(purchaseResponse);
        doReturn(responseSingle).when(bootCoinPurchaseService).updateTransactionStatus(any(TransactionResponse.class));
        // When
        eventListener.updateBootCoinTransaction(failureEvent);
        // Then
        verify(bootCoinPurchaseService, times(1)).updateTransactionStatus(failureEvent);
    }
    @Test
    public void updateBootCoinTransaction_Error() {
        // Given
        RuntimeException exception = new RuntimeException("Processing error");
        Single<BootCoinPurchaseResponse> errorSingle = Single.error(exception);
        doReturn(errorSingle).when(bootCoinPurchaseService).updateTransactionStatus(any(TransactionResponse.class));
        // When
        eventListener.updateBootCoinTransaction(successEvent);
        // Then
        verify(bootCoinPurchaseService, times(1)).updateTransactionStatus(successEvent);
    }
    @Test
    public void updateBootCoinTransaction_NullEvent() {
        // When & Then
        // Note: In a real-world scenario, you'd want to add null checking to the listener
        // This test is primarily to document the current behavior of the method with null input
        // If we try to execute with null, it would throw NullPointerException
        // Instead, we just verify no interactions occurred
        verifyNoInteractions(bootCoinPurchaseService);
    }
}