package com.bank.bootcoinservice.service.user;

import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountRequest;
import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountResponse;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiRequest;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiResponse;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationRequest;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationResponse;
import com.bank.bootcoinservice.dto.event.KafkaValidationRequest;
import com.bank.bootcoinservice.event.BootCoinUserEventListener;
import com.bank.bootcoinservice.model.user.BootCoinUser;
import com.bank.bootcoinservice.repository.BootCoinUserRepository;
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
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BootCoinUserServiceImplTest {
    @Mock
    private BootCoinUserRepository repository;
    @Mock
    private KafkaTemplate<String, KafkaValidationRequest> kafkaTemplate;
    @Mock
    private BootCoinUserEventListener eventListener;
    @InjectMocks
    private BootCoinUserServiceImpl bootCoinUserService;
    @Captor
    private ArgumentCaptor<BootCoinUser> userCaptor;
    private BootCoinUserRegistrationRequest registrationRequest;
    private BootCoinUser savedUser;
    private AssociateYankiRequest yankiRequest;
    private AssociateBankAccountRequest bankAccountRequest;
    @BeforeEach
    void setUp() {
        // Initialize registration request
        registrationRequest = new BootCoinUserRegistrationRequest();
        registrationRequest.setDocumentNumber("12345678");
        registrationRequest.setPhoneNumber("999888777");
        registrationRequest.setEmail("user@example.com");
        registrationRequest.setAssociateYanki(false);
        registrationRequest.setBankAccountId(null);
        // Initialize saved user
        savedUser = new BootCoinUser();
        savedUser.setId(UUID.randomUUID().toString());
        savedUser.setDocumentNumber(registrationRequest.getDocumentNumber());
        savedUser.setPhoneNumber(registrationRequest.getPhoneNumber());
        savedUser.setEmail(registrationRequest.getEmail());
        savedUser.setBalance(BigDecimal.ZERO);
        savedUser.setHasYanki(false);
        savedUser.setBankAccountId(null);
        // Initialize Yanki request
        yankiRequest = new AssociateYankiRequest();
        yankiRequest.setDocumentNumber("12345678");
        // Initialize bank account request
        bankAccountRequest = new AssociateBankAccountRequest();
        bankAccountRequest.setDocumentNumber("12345678");
        bankAccountRequest.setBankAccountId("bank123");
    }
    @Test
    public void registerUser_Basic() {
        // Given
        when(repository.save(any(BootCoinUser.class))).thenReturn(Single.just(savedUser));
        // When
        BootCoinUserRegistrationResponse response = bootCoinUserService
                .registerUser(registrationRequest)
                .blockingGet();
        // Then
        verify(repository).save(userCaptor.capture());
        BootCoinUser capturedUser = userCaptor.getValue();
        assertEquals(registrationRequest.getDocumentNumber(), capturedUser.getDocumentNumber());
        assertEquals(registrationRequest.getPhoneNumber(), capturedUser.getPhoneNumber());
        assertEquals(registrationRequest.getEmail(), capturedUser.getEmail());
        assertEquals(BigDecimal.ZERO, capturedUser.getBalance());
        assertEquals(registrationRequest.isAssociateYanki(), capturedUser.isHasYanki());
        assertEquals(registrationRequest.getBankAccountId(), capturedUser.getBankAccountId());
        assertEquals(savedUser.getId(), response.getUserId());
        assertEquals(savedUser.isHasYanki(), response.isHasYanki());
        assertEquals(savedUser.getBankAccountId(), response.getBankAccountId());
        // Verify no event listener interactions for basic registration
        verifyNoInteractions(eventListener);
    }
    @Test
    public void registerUser_WithYanki() {
        // Given
        registrationRequest.setAssociateYanki(true);
        savedUser.setHasYanki(true);
        when(repository.save(any(BootCoinUser.class))).thenReturn(Single.just(savedUser));
        when(eventListener.sendEventAndWait(anyString(), any(KafkaValidationRequest.class)))
                .thenReturn(Single.just(true));
        // When
        BootCoinUserRegistrationResponse response = bootCoinUserService
                .registerUser(registrationRequest)
                .blockingGet();
        // Then
        verify(repository).save(userCaptor.capture());
        verify(eventListener).sendEventAndWait(eq("bootcoin.yanki.association"), any(KafkaValidationRequest.class));
        BootCoinUser capturedUser = userCaptor.getValue();
        assertTrue(capturedUser.isHasYanki());
        assertEquals(savedUser.getId(), response.getUserId());
        assertTrue(response.isHasYanki());
        assertNull(response.getBankAccountId());
    }
    @Test
    public void registerUser_WithBankAccount() {
        // Given
        String bankAccountId = "bank123";
        registrationRequest.setBankAccountId(bankAccountId);
        savedUser.setBankAccountId(bankAccountId);
        when(repository.save(any(BootCoinUser.class))).thenReturn(Single.just(savedUser));
        when(eventListener.sendEventAndWait(anyString(), any(KafkaValidationRequest.class)))
                .thenReturn(Single.just(true));
        // When
        BootCoinUserRegistrationResponse response = bootCoinUserService
                .registerUser(registrationRequest)
                .blockingGet();
        // Then
        verify(repository).save(userCaptor.capture());
        verify(eventListener).sendEventAndWait(eq("bootcoin.bank.account.association"),
            any(KafkaValidationRequest.class));
        BootCoinUser capturedUser = userCaptor.getValue();
        assertEquals(bankAccountId, capturedUser.getBankAccountId());
        assertEquals(savedUser.getId(), response.getUserId());
        assertFalse(response.isHasYanki());
        assertEquals(bankAccountId, response.getBankAccountId());
    }
    @Test
    public void registerUser_WithBothYankiAndBankAccount() {
        // Given
        String bankAccountId = "bank123";
        registrationRequest.setAssociateYanki(true);
        registrationRequest.setBankAccountId(bankAccountId);
        savedUser.setHasYanki(true);
        savedUser.setBankAccountId(bankAccountId);
        when(repository.save(any(BootCoinUser.class))).thenReturn(Single.just(savedUser));
        when(eventListener.sendEventAndWait(anyString(), any(KafkaValidationRequest.class)))
                .thenReturn(Single.just(true));
        // When
        BootCoinUserRegistrationResponse response = bootCoinUserService
                .registerUser(registrationRequest)
                .blockingGet();
        // Then
        verify(repository).save(userCaptor.capture());
        verify(eventListener, times(2)).sendEventAndWait(anyString(), any(KafkaValidationRequest.class));
        BootCoinUser capturedUser = userCaptor.getValue();
        assertTrue(capturedUser.isHasYanki());
        assertEquals(bankAccountId, capturedUser.getBankAccountId());
        assertEquals(savedUser.getId(), response.getUserId());
        assertTrue(response.isHasYanki());
        assertEquals(bankAccountId, response.getBankAccountId());
    }
    @Test
    public void associateBankAccount_Success() {
        // Given
        BootCoinUser user = new BootCoinUser();
        user.setId(UUID.randomUUID().toString());
        user.setDocumentNumber(bankAccountRequest.getDocumentNumber());
        user.setBankAccountId(null);
        BootCoinUser updatedUser = new BootCoinUser();
        updatedUser.setId(user.getId());
        updatedUser.setDocumentNumber(user.getDocumentNumber());
        updatedUser.setBankAccountId(bankAccountRequest.getBankAccountId());
        Maybe<BootCoinUser> userMaybe = Maybe.just(user);
        Single<Boolean> validationSingle = Single.just(true);
        Single<BootCoinUser> updatedUserSingle = Single.just(updatedUser);
        doReturn(userMaybe).when(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        doReturn(validationSingle).when(eventListener).sendEventAndWait(anyString(), any(KafkaValidationRequest.class));
        doReturn(updatedUserSingle).when(repository).save(any(BootCoinUser.class));
        // When
        AssociateBankAccountResponse response = bootCoinUserService
                .associateBankAccount(bankAccountRequest)
                .blockingGet();
        // Then
        verify(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        verify(eventListener).sendEventAndWait(eq("bootcoin.bank.account.association"),
            any(KafkaValidationRequest.class));
        verify(repository).save(userCaptor.capture());
        BootCoinUser capturedUser = userCaptor.getValue();
        assertEquals(bankAccountRequest.getBankAccountId(), capturedUser.getBankAccountId());
        assertEquals(updatedUser.getId(), response.getDocumentNumber());
        assertEquals(bankAccountRequest.getBankAccountId(), response.getBankAccountId());
    }
    @Test
    public void associateBankAccount_UserNotFound() {
        // Given
        Maybe<BootCoinUser> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            bootCoinUserService.associateBankAccount(bankAccountRequest).blockingGet();
        });
        verify(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        verifyNoInteractions(eventListener);
    }
    @Test
    public void associateBankAccount_AlreadyAssociated() {
        // Given
        BootCoinUser user = new BootCoinUser();
        user.setDocumentNumber(bankAccountRequest.getDocumentNumber());
        user.setBankAccountId("existing-bank-account");
        Maybe<BootCoinUser> userMaybe = Maybe.just(user);
        doReturn(userMaybe).when(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bootCoinUserService.associateBankAccount(bankAccountRequest).blockingGet();
        });
        assertEquals("User already has a bank account associated", exception.getMessage());
        verify(repository).findByDocumentNumber(bankAccountRequest.getDocumentNumber());
        verifyNoInteractions(eventListener);
    }
    @Test
    public void validateUser_Valid() {
        // Given
        String phoneNumber = "999888777";
        String documentNumber = "12345678";
        BootCoinUser user = new BootCoinUser();
        user.setPhoneNumber(phoneNumber);
        user.setDocumentNumber(documentNumber);
        Maybe<BootCoinUser> userMaybe = Maybe.just(user);
        doReturn(userMaybe).when(repository).findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber);
        // When
        Boolean result = bootCoinUserService
                .validateUser(phoneNumber, documentNumber)
                .blockingGet();
        // Then
        assertTrue(result);
        verify(repository).findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber);
    }
    @Test
    public void validateUser_Invalid() {
        // Given
        String phoneNumber = "999888777";
        String documentNumber = "12345678";
        Maybe<BootCoinUser> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(repository).findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber);
        // When
        Boolean result = bootCoinUserService
                .validateUser(phoneNumber, documentNumber)
                .blockingGet();
        // Then
        assertFalse(result);
        verify(repository).findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber);
    }
    @Test
    public void associateYanki_Success() {
        // Given
        BootCoinUser user = new BootCoinUser();
        user.setId(UUID.randomUUID().toString());
        user.setDocumentNumber(yankiRequest.getDocumentNumber());
        user.setPhoneNumber("999888777");
        user.setHasYanki(false);
        BootCoinUser updatedUser = new BootCoinUser();
        updatedUser.setId(user.getId());
        updatedUser.setDocumentNumber(user.getDocumentNumber());
        updatedUser.setPhoneNumber(user.getPhoneNumber());
        updatedUser.setHasYanki(true);
        Maybe<BootCoinUser> userMaybe = Maybe.just(user);
        Single<Boolean> validationSingle = Single.just(true);
        Single<BootCoinUser> updatedUserSingle = Single.just(updatedUser);
        doReturn(userMaybe).when(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        doReturn(validationSingle).when(eventListener).sendEventAndWait(anyString(), any(KafkaValidationRequest.class));
        doReturn(updatedUserSingle).when(repository).save(any(BootCoinUser.class));
        // When
        AssociateYankiResponse response = bootCoinUserService
                .associateYanki(yankiRequest)
                .blockingGet();
        // Then
        verify(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        verify(eventListener).sendEventAndWait(eq("bootcoin.yanki.association"), any(KafkaValidationRequest.class));
        verify(repository).save(userCaptor.capture());
        BootCoinUser capturedUser = userCaptor.getValue();
        assertTrue(capturedUser.isHasYanki());
        assertEquals(updatedUser.getId(), response.getDocumentNumber());
        assertTrue(response.isHasYanki());
    }
    @Test
    public void associateYanki_UserNotFound() {
        // Given
        Maybe<BootCoinUser> emptyMaybe = Maybe.empty();
        doReturn(emptyMaybe).when(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            bootCoinUserService.associateYanki(yankiRequest).blockingGet();
        });
        verify(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        verifyNoInteractions(eventListener);
    }
    @Test
    public void associateYanki_AlreadyAssociated() {
        // Given
        BootCoinUser user = new BootCoinUser();
        user.setDocumentNumber(yankiRequest.getDocumentNumber());
        user.setHasYanki(true);
        Maybe<BootCoinUser> userMaybe = Maybe.just(user);
        doReturn(userMaybe).when(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bootCoinUserService.associateYanki(yankiRequest).blockingGet();
        });
        assertEquals("User already has a Yanki account associated", exception.getMessage());
        verify(repository).findByDocumentNumber(yankiRequest.getDocumentNumber());
        verifyNoInteractions(eventListener);
    }
}