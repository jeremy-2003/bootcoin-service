package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.transaction.bank.BootCoinBankPurchaseCompleted;
import com.bank.bootcoinservice.service.transaction.BootCoinBankTransactionService;
import io.reactivex.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BootCoinBankTransactionEventListenerTest {
    @Mock
    private BootCoinBankTransactionService bootCoinBankTransactionService;
    @InjectMocks
    private BootCoinBankTransactionEventListener eventListener;
    private BootCoinBankPurchaseCompleted event;
    @BeforeEach
    void setUp() {
        // Initialize test data
        event = BootCoinBankPurchaseCompleted.builder()
                .transactionId("transaction123")
                .accepted(true)
                .build();
    }
    @Test
    public void updateTransactionBootCoinBank_Success() {
        // Given
        when(bootCoinBankTransactionService.processTransactionResult(any(BootCoinBankPurchaseCompleted.class)))
                .thenReturn(Completable.complete());
        // When
        eventListener.updateTransactionBootCoinBank(event);
        // Then
        verify(bootCoinBankTransactionService, times(1)).processTransactionResult(event);
    }
    @Test
    public void updateTransactionBootCoinBank_Error() {
        // Given
        RuntimeException exception = new RuntimeException("Processing error");
        when(bootCoinBankTransactionService.processTransactionResult(any(BootCoinBankPurchaseCompleted.class)))
                .thenReturn(Completable.error(exception));
        // When
        eventListener.updateTransactionBootCoinBank(event);
        // Then
        verify(bootCoinBankTransactionService, times(1)).processTransactionResult(event);
        // We don't need to verify anything else as the error is handled by onErrorComplete()
        // and the test would fail if the error is not properly handled
    }
    @Test
    public void updateTransactionBootCoinBank_RejectedTransaction() {
        // Given
        BootCoinBankPurchaseCompleted rejectedEvent = BootCoinBankPurchaseCompleted.builder()
                .transactionId("transaction456")
                .accepted(false)
                .build();
        when(bootCoinBankTransactionService.processTransactionResult(any(BootCoinBankPurchaseCompleted.class)))
                .thenReturn(Completable.complete());
        // When
        eventListener.updateTransactionBootCoinBank(rejectedEvent);
        // Then
        verify(bootCoinBankTransactionService, times(1)).processTransactionResult(rejectedEvent);
    }
}