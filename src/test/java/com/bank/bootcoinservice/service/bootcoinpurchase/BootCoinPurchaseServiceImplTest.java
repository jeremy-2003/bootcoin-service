package com.bank.bootcoinservice.service.bootcoinpurchase;

import com.bank.bootcoinservice.dto.bootcoinpurchase.*;
import com.bank.bootcoinservice.model.bootcoinpurchase.BootCoinPurchase;
import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import com.bank.bootcoinservice.model.transaction.BootCoinTransaction;
import com.bank.bootcoinservice.model.transaction.PaymentMethod;
import com.bank.bootcoinservice.model.transaction.TransactionStatus;
import com.bank.bootcoinservice.model.transaction.TransactionType;
import com.bank.bootcoinservice.model.user.BootCoinUser;
import com.bank.bootcoinservice.repository.BootCoinPurchaseRepository;
import com.bank.bootcoinservice.repository.BootCoinTransactionRepository;
import com.bank.bootcoinservice.repository.BootCoinUserRepository;
import com.bank.bootcoinservice.service.exchangerate.BootCoinExchangeRateService;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BootCoinPurchaseServiceImplTest {

    @Mock
    private BootCoinPurchaseRepository purchaseRepository;

    @Mock
    private BootCoinUserRepository userRepository;

    @Mock
    private BootCoinTransactionRepository transactionRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private BootCoinExchangeRateService exchangeRateService;

    @InjectMocks
    private BootCoinPurchaseServiceImpl purchaseService;

    @Captor
    private ArgumentCaptor<BootCoinPurchase> purchaseCaptor;

    @Captor
    private ArgumentCaptor<BootCoinTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<TransactionEvent> transferEventCaptor;

    @Captor
    private ArgumentCaptor<YankiEvent> yankiEventCaptor;

    private BootCoinUser buyer;
    private BootCoinUser seller;
    private BootCoinPurchaseRequest purchaseRequest;
    private BootCoinSellRequest sellRequest;
    private BootCoinPurchase savedPurchase;
    private BootCoinExchangeRate exchangeRate;
    private TransactionResponse successResponse;
    private TransactionResponse failureResponse;
    private String purchaseId;

    @BeforeEach
    void setUp() {
        // IDs
        purchaseId = UUID.randomUUID().toString();
        String buyerId = UUID.randomUUID().toString();
        String sellerId = UUID.randomUUID().toString();

        // Setup buyer
        buyer = new BootCoinUser();
        buyer.setId(buyerId);
        buyer.setDocumentNumber("12345678");
        buyer.setPhoneNumber("999888777");
        buyer.setEmail("buyer@example.com");
        buyer.setBalance(new BigDecimal("100.00"));
        buyer.setHasYanki(true);
        buyer.setBankAccountId("buyer-account-123");

        // Setup seller
        seller = new BootCoinUser();
        seller.setId(sellerId);
        seller.setDocumentNumber("87654321");
        seller.setPhoneNumber("777888999");
        seller.setEmail("seller@example.com");
        seller.setBalance(new BigDecimal("50.00"));
        seller.setHasYanki(true);
        seller.setBankAccountId("seller-account-456");

        // Setup purchase request
        purchaseRequest = BootCoinPurchaseRequest.builder()
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .paymentMethod(PaymentMethod.TRANSFER)
                .amount(new BigDecimal("10.00"))
                .build();

        // Setup sell request
        sellRequest = BootCoinSellRequest.builder()
                .sellerDocumentNumber(seller.getDocumentNumber())
                .sellerAccountNumber(seller.getBankAccountId())
                .build();

        // Setup exchange rate
        exchangeRate = new BootCoinExchangeRate();
        exchangeRate.setBuyRate(new BigDecimal("3.75"));
        exchangeRate.setSellRate(new BigDecimal("3.85"));
        exchangeRate.setUpdatedAt(LocalDateTime.now());

        // Setup saved purchase
        savedPurchase = BootCoinPurchase.builder()
                .id(purchaseId)
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .buyerPhoneNumber(buyer.getPhoneNumber())
                .buyerAccountNumber(buyer.getBankAccountId())
                .paymentMethod(PaymentMethod.TRANSFER)
                .amount(new BigDecimal("10.00"))
                .totalAmountInPEN(new BigDecimal("38.50")) // 10 * 3.85
                .status(TransactionStatus.WAITING_FOR_SELLER)
                .transactionType(TransactionType.P2P)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup transaction responses
        successResponse = new TransactionResponse();
        successResponse.setTransactionId(purchaseId);
        successResponse.setSuccess(true);
        successResponse.setMessage("Transaction completed successfully");

        failureResponse = new TransactionResponse();
        failureResponse.setTransactionId(purchaseId);
        failureResponse.setSuccess(false);
        failureResponse.setMessage("Transaction failed: insufficient funds");
    }

    @Test
    void requestPurchase_Success() {
        // Given
        Maybe<BootCoinUser> buyerMaybe = Maybe.just(buyer);
        Single<BootCoinExchangeRate> rateSingle = Single.just(exchangeRate);
        Single<BootCoinPurchase> purchaseSingle = Single.just(savedPurchase);

        doReturn(buyerMaybe).when(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        doReturn(rateSingle).when(exchangeRateService).getCurrentExchangeRate();
        doReturn(purchaseSingle).when(purchaseRepository).save(any(BootCoinPurchase.class));

        // When
        BootCoinPurchaseResponse response = purchaseService.requestPurchase(purchaseRequest).blockingGet();

        // Then
        verify(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        verify(exchangeRateService).getCurrentExchangeRate();
        verify(purchaseRepository).save(purchaseCaptor.capture());

        BootCoinPurchase capturedPurchase = purchaseCaptor.getValue();
        assertEquals(buyer.getDocumentNumber(), capturedPurchase.getBuyerDocumentNumber());
        assertEquals(purchaseRequest.getPaymentMethod(), capturedPurchase.getPaymentMethod());
        assertEquals(purchaseRequest.getAmount(), capturedPurchase.getAmount());
        assertEquals(TransactionStatus.WAITING_FOR_SELLER, capturedPurchase.getStatus());
        assertEquals(TransactionType.P2P, capturedPurchase.getTransactionType());

        assertEquals(savedPurchase.getId(), response.getPurchaseId());
        assertEquals(savedPurchase.getBuyerDocumentNumber(), response.getBuyerDocumentNumber());
        assertEquals(savedPurchase.getAmount(), response.getAmount());
        assertEquals(savedPurchase.getStatus(), response.getStatus());
    }

    @Test
    void requestPurchase_UserNotFound() {
        // Given
        Maybe<BootCoinUser> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(userRepository).findByDocumentNumber(buyer.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.requestPurchase(purchaseRequest).blockingGet();
        });

        assertEquals("User not found.", exception.getMessage());
        verify(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        verifyNoInteractions(exchangeRateService);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void requestPurchase_YankiNotEnabled() {
        // Given
        buyer.setHasYanki(false);
        purchaseRequest.setPaymentMethod(PaymentMethod.YANKI);

        Maybe<BootCoinUser> buyerMaybe = Maybe.just(buyer);
        doReturn(buyerMaybe).when(userRepository).findByDocumentNumber(buyer.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.requestPurchase(purchaseRequest).blockingGet();
        });

        assertEquals("User does not have Yanki enabled.", exception.getMessage());
        verify(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        verifyNoInteractions(exchangeRateService);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void requestPurchase_NoBankAccount() {
        // Given
        buyer.setBankAccountId(null);
        purchaseRequest.setPaymentMethod(PaymentMethod.TRANSFER);

        Maybe<BootCoinUser> buyerMaybe = Maybe.just(buyer);
        doReturn(buyerMaybe).when(userRepository).findByDocumentNumber(buyer.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.requestPurchase(purchaseRequest).blockingGet();
        });

        assertEquals("User does not have a valid bank account.", exception.getMessage());
        verify(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        verifyNoInteractions(exchangeRateService);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void getPendingPurchases_Success() {
        // Given
        BootCoinPurchase purchase1 = BootCoinPurchase.builder()
                .id(UUID.randomUUID().toString())
                .status(TransactionStatus.WAITING_FOR_SELLER)
                .build();
        BootCoinPurchase purchase2 = BootCoinPurchase.builder()
                .id(UUID.randomUUID().toString())
                .status(TransactionStatus.WAITING_FOR_SELLER)
                .build();

        Flowable<BootCoinPurchase> purchasesFlowable = Flowable.fromIterable(Arrays.asList(purchase1, purchase2));
        doReturn(purchasesFlowable).when(purchaseRepository).findByStatus(TransactionStatus.WAITING_FOR_SELLER);

        // When
        List<BootCoinPurchaseResponse> responses = purchaseService.getPendingPurchases()
                .toList()
                .blockingGet();

        // Then
        assertEquals(2, responses.size());
        verify(purchaseRepository).findByStatus(TransactionStatus.WAITING_FOR_SELLER);
    }

    @Test
    void acceptPurchase_Success_Transfer() {
        // Given
        // Update savedPurchase with necessary details
        savedPurchase.setStatus(TransactionStatus.WAITING_FOR_SELLER);

        // Create updated purchase with seller info
        BootCoinPurchase updatedPurchase = BootCoinPurchase.builder()
                .id(savedPurchase.getId())
                .buyerDocumentNumber(savedPurchase.getBuyerDocumentNumber())
                .buyerPhoneNumber(savedPurchase.getBuyerPhoneNumber())
                .buyerAccountNumber(savedPurchase.getBuyerAccountNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .sellerAccountNumber(seller.getBankAccountId())
                .paymentMethod(PaymentMethod.TRANSFER)
                .amount(savedPurchase.getAmount())
                .totalAmountInPEN(savedPurchase.getTotalAmountInPEN())
                .status(TransactionStatus.PROCESSING)
                .transactionType(TransactionType.P2P)
                .createdAt(savedPurchase.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(savedPurchase);
        Maybe<BootCoinUser> sellerMaybe = Maybe.just(seller);
        Single<BootCoinPurchase> updatedPurchaseSingle = Single.just(updatedPurchase);

        doReturn(purchaseMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        doReturn(sellerMaybe).when(userRepository)
            .findByDocumentNumber(seller.getDocumentNumber());
        doReturn(updatedPurchaseSingle).when(purchaseRepository)
            .save(any(BootCoinPurchase.class));
        doReturn(null).when(kafkaTemplate).send(anyString(), any());

        // When
        BootCoinPurchaseResponse response = purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();

        // Then
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verify(kafkaTemplate).send(eq("bootcoin.transaction.transfer.requested"), transferEventCaptor.capture());

        BootCoinPurchase capturedPurchase = purchaseCaptor.getValue();
        assertEquals(seller.getDocumentNumber(), capturedPurchase.getSellerDocumentNumber());
        assertEquals(seller.getBankAccountId(), capturedPurchase.getSellerAccountNumber());
        assertEquals(TransactionStatus.PROCESSING, capturedPurchase.getStatus());

        TransactionEvent capturedEvent = transferEventCaptor.getValue();
        assertEquals(savedPurchase.getId(), capturedEvent.getPurchaseId());
        assertEquals(buyer.getDocumentNumber(), capturedEvent.getBuyerDocumentNumber());
        assertEquals(seller.getDocumentNumber(), capturedEvent.getSellerDocumentNumber());
        assertEquals(savedPurchase.getAmount(), capturedEvent.getAmount());
    }

    @Test
    void acceptPurchase_Success_Yanki() {
        // Given
        // Update savedPurchase with Yanki payment method
        savedPurchase.setPaymentMethod(PaymentMethod.YANKI);
        savedPurchase.setStatus(TransactionStatus.WAITING_FOR_SELLER);

        // Update sell request for Yanki
        sellRequest = BootCoinSellRequest.builder()
                .sellerDocumentNumber(seller.getDocumentNumber())
                .sellerPhoneNumber(seller.getPhoneNumber())
                .build();

        // Create updated purchase with seller info
        BootCoinPurchase updatedPurchase = BootCoinPurchase.builder()
                .id(savedPurchase.getId())
                .buyerDocumentNumber(savedPurchase.getBuyerDocumentNumber())
                .buyerPhoneNumber(savedPurchase.getBuyerPhoneNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .sellerPhoneNumber(seller.getPhoneNumber())
                .paymentMethod(PaymentMethod.YANKI)
                .amount(savedPurchase.getAmount())
                .totalAmountInPEN(savedPurchase.getTotalAmountInPEN())
                .status(TransactionStatus.PROCESSING)
                .transactionType(TransactionType.P2P)
                .createdAt(savedPurchase.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(savedPurchase);
        Maybe<BootCoinUser> sellerMaybe = Maybe.just(seller);
        Single<BootCoinPurchase> updatedPurchaseSingle = Single.just(updatedPurchase);

        doReturn(purchaseMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        doReturn(sellerMaybe).when(userRepository)
            .findByDocumentNumber(seller.getDocumentNumber());
        doReturn(updatedPurchaseSingle).when(purchaseRepository)
            .save(any(BootCoinPurchase.class));
        doReturn(null).when(kafkaTemplate).send(anyString(), any());

        // When
        BootCoinPurchaseResponse response = purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();

        // Then
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verify(kafkaTemplate).send(eq("bootcoin.transaction.yanki.requested"), yankiEventCaptor.capture());

        BootCoinPurchase capturedPurchase = purchaseCaptor.getValue();
        assertEquals(seller.getDocumentNumber(), capturedPurchase.getSellerDocumentNumber());
        assertEquals(seller.getPhoneNumber(), capturedPurchase.getSellerPhoneNumber());
        assertEquals(TransactionStatus.PROCESSING, capturedPurchase.getStatus());

        YankiEvent capturedEvent = yankiEventCaptor.getValue();
        assertEquals(savedPurchase.getId(), capturedEvent.getPurchaseId());
        assertEquals(buyer.getDocumentNumber(), capturedEvent.getBuyerDocumentNumber());
        assertEquals(seller.getDocumentNumber(), capturedEvent.getSellerDocumentNumber());
        assertEquals(savedPurchase.getAmount(), capturedEvent.getAmount());
    }

    @Test
    void acceptPurchase_PurchaseNotFound() {
        // Given
        Maybe<BootCoinPurchase> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();
        });

        assertEquals("Purchase request not found or already taken.", exception.getMessage());
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(purchaseRepository);
    }

    @Test
    void acceptPurchase_SellerNotFound() {
        // Given
        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(savedPurchase);
        Maybe<BootCoinUser> emptyMaybe = Maybe.empty();

        doReturn(purchaseMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        doReturn(emptyMaybe).when(userRepository)
            .findByDocumentNumber(seller.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();
        });

        assertEquals("Seller not found.", exception.getMessage());
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verifyNoMoreInteractions(purchaseRepository);
    }

    @Test
    void acceptPurchase_SellerYankiNotEnabled() {
        // Given
        savedPurchase.setPaymentMethod(PaymentMethod.YANKI);
        seller.setHasYanki(false);

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(savedPurchase);
        Maybe<BootCoinUser> sellerMaybe = Maybe.just(seller);

        doReturn(purchaseMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        doReturn(sellerMaybe).when(userRepository)
            .findByDocumentNumber(seller.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();
        });

        assertEquals("Seller does not have Yanki enabled.", exception.getMessage());
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verifyNoMoreInteractions(purchaseRepository);
    }

    @Test
    void acceptPurchase_SellerLowBalance() {
        // Given
        // Set seller balance lower than purchase amount
        seller.setBalance(new BigDecimal("5.00"));
        savedPurchase.setAmount(new BigDecimal("10.00"));

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(savedPurchase);
        Maybe<BootCoinUser> sellerMaybe = Maybe.just(seller);

        doReturn(purchaseMaybe).when(purchaseRepository)
            .findByIdAndStatus(purchaseId,
                TransactionStatus.WAITING_FOR_SELLER);
        doReturn(sellerMaybe).when(userRepository)
            .findByDocumentNumber(seller.getDocumentNumber());

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.acceptPurchase(purchaseId, sellRequest).blockingGet();
        });

        assertEquals("Seller does not have enough balance to take the offer.", exception.getMessage());
        verify(purchaseRepository).findByIdAndStatus(purchaseId, TransactionStatus.WAITING_FOR_SELLER);
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verifyNoMoreInteractions(purchaseRepository);
    }

    @Test
    void updateTransactionStatus_Success() {
        // Given
        BootCoinPurchase purchase = BootCoinPurchase.builder()
                .id(purchaseId)
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.PROCESSING)
                .build();

        BootCoinPurchase updatedPurchase = BootCoinPurchase.builder()
                .id(purchaseId)
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.COMPLETED)
                .messageResponse("Transaction completed successfully")
                .build();

        BootCoinTransaction transaction = new BootCoinTransaction();
        transaction.setId(purchaseId);

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(purchase);
        Single<BootCoinPurchase> updatedPurchaseSingle = Single.just(updatedPurchase);
        Single<BootCoinTransaction> transactionSingle = Single.just(transaction);
        Maybe<BootCoinUser> buyerMaybe = Maybe.just(buyer);
        Maybe<BootCoinUser> sellerMaybe = Maybe.just(seller);

        doReturn(purchaseMaybe).when(purchaseRepository).findById(purchaseId);
        doReturn(updatedPurchaseSingle).when(purchaseRepository).save(any(BootCoinPurchase.class));
        doReturn(transactionSingle).when(transactionRepository).save(any(BootCoinTransaction.class));
        doReturn(buyerMaybe).when(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        doReturn(sellerMaybe).when(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        doReturn(Single.just(buyer)).when(userRepository).save(any(BootCoinUser.class));

        // When
        BootCoinPurchase result = purchaseService.updateTransactionStatus(successResponse).blockingGet();

        // Then
        verify(purchaseRepository).findById(purchaseId);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(userRepository).findByDocumentNumber(buyer.getDocumentNumber());
        verify(userRepository).findByDocumentNumber(seller.getDocumentNumber());
        verify(userRepository, times(2)).save(any(BootCoinUser.class));

        BootCoinPurchase capturedPurchase = purchaseCaptor.getValue();
        assertEquals(TransactionStatus.COMPLETED, capturedPurchase.getStatus());
        assertEquals("Transaction completed successfully", capturedPurchase.getMessageResponse());

        BootCoinTransaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(purchaseId, capturedTransaction.getId());
        assertEquals(TransactionStatus.COMPLETED, capturedTransaction.getStatus());
    }

    @Test
    void updateTransactionStatus_Failed() {
        // Given
        BootCoinPurchase purchase = BootCoinPurchase.builder()
                .id(purchaseId)
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.PROCESSING)
                .build();

        BootCoinPurchase updatedPurchase = BootCoinPurchase.builder()
                .id(purchaseId)
                .buyerDocumentNumber(buyer.getDocumentNumber())
                .sellerDocumentNumber(seller.getDocumentNumber())
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.FAILED)
                .messageResponse("Transaction failed: insufficient funds")
                .build();

        Maybe<BootCoinPurchase> purchaseMaybe = Maybe.just(purchase);
        Single<BootCoinPurchase> updatedPurchaseSingle = Single.just(updatedPurchase);

        doReturn(purchaseMaybe).when(purchaseRepository).findById(purchaseId);
        doReturn(updatedPurchaseSingle).when(purchaseRepository).save(any(BootCoinPurchase.class));

        // When
        BootCoinPurchase result = purchaseService.updateTransactionStatus(failureResponse).blockingGet();

        // Then
        verify(purchaseRepository).findById(purchaseId);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(userRepository);

        BootCoinPurchase capturedPurchase = purchaseCaptor.getValue();
        assertEquals(TransactionStatus.FAILED, capturedPurchase.getStatus());
        assertEquals("Transaction failed: insufficient funds", capturedPurchase.getMessageResponse());
    }

    @Test
    void updateTransactionStatus_TransactionNotFound() {
        // Given
        Maybe<BootCoinPurchase> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(purchaseRepository).findById(purchaseId);

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.updateTransactionStatus(successResponse).blockingGet();
        });

        assertEquals("Transaction not found.", exception.getMessage());
        verify(purchaseRepository).findById(purchaseId);
        verifyNoMoreInteractions(purchaseRepository);
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(userRepository);
    }
}
