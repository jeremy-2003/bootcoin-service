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
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BootCoinUserControllerTest {
    @Mock
    private BootCoinUserService bootCoinUserService;
    @Mock
    private JwtProvider jwtProvider;
    @InjectMocks
    private BootCoinUserController bootCoinUserController;
    private BootCoinUserRegistrationRequest registrationRequest;
    private BootCoinUserRegistrationResponse registrationResponse;
    private LoginRequest loginRequest;
    private AssociateYankiRequest yankiRequest;
    private AssociateYankiResponse yankiResponse;
    private AssociateBankAccountRequest bankAccountRequest;
    private AssociateBankAccountResponse bankAccountResponse;
    private String validToken;
    @BeforeEach
    void setUp() {
        registrationRequest = new BootCoinUserRegistrationRequest();
        registrationRequest.setDocumentNumber("12345678");
        registrationRequest.setPhoneNumber("999888777");
        registrationResponse = new BootCoinUserRegistrationResponse();
        registrationResponse.setUserId("user123");
        registrationResponse.setHasYanki(false);
        registrationResponse.setBankAccountId(null);
        loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber("999888777");
        loginRequest.setDocumentNumber("12345678");
        yankiRequest = new AssociateYankiRequest();
        yankiRequest.setDocumentNumber("999888777");
        yankiResponse = new AssociateYankiResponse();
        yankiResponse.setDocumentNumber("12345678");
        yankiResponse.setHasYanki(true);
        bankAccountRequest = new AssociateBankAccountRequest();
        bankAccountRequest.setDocumentNumber("123456789");
        bankAccountResponse = new AssociateBankAccountResponse();
        bankAccountResponse.setDocumentNumber("12345678");
        bankAccountResponse.setBankAccountId("bank123");
        validToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ" +
                ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
    @Test
    public void registerUser_Success() {
        // Given
        when(bootCoinUserService.registerUser(any(BootCoinUserRegistrationRequest.class)))
                .thenReturn(Single.just(registrationResponse));
        // When
        ResponseEntity<BaseResponse<BootCoinUserRegistrationResponse>> result =
                bootCoinUserController.registerUser(registrationRequest).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.CREATED.value(), result.getBody().getStatus());
        assertEquals("User registered successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(registrationResponse.getUserId(), result.getBody().getData().getUserId());
    }
    @Test
    public void registerUser_Error() {
        // Given
        String errorMessage = "Registration failed";
        when(bootCoinUserService.registerUser(any(BootCoinUserRegistrationRequest.class)))
                .thenReturn(Single.error(new RuntimeException(errorMessage)));
        // When
        ResponseEntity<BaseResponse<BootCoinUserRegistrationResponse>> result =
                bootCoinUserController.registerUser(registrationRequest).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getBody().getStatus());
        assertEquals(errorMessage, result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void login_Success() {
        // Given
        when(bootCoinUserService.validateUser(anyString(), anyString()))
                .thenReturn(Single.just(true));
        when(jwtProvider.generateToken(anyString()))
                .thenReturn("jwt-token");
        // When
        ResponseEntity<BaseResponse<String>> result =
                bootCoinUserController.login(loginRequest).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(200, result.getBody().getStatus());
        assertEquals("Success login", result.getBody().getMessage());
        assertEquals("jwt-token", result.getBody().getData());
    }
    @Test
    public void login_InvalidCredentials() {
        // Given
        when(bootCoinUserService.validateUser(anyString(), anyString()))
                .thenReturn(Single.just(false));
        // When
        ResponseEntity<BaseResponse<String>> result =
                bootCoinUserController.login(loginRequest).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Invalid credentials", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void associateYanki_Success() {
        // Given
        when(bootCoinUserService.associateYanki(any(AssociateYankiRequest.class)))
                .thenReturn(Single.just(yankiResponse));
        // When
        ResponseEntity<BaseResponse<AssociateYankiResponse>> result =
                bootCoinUserController.associateYanki(yankiRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Yanki associated successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(yankiResponse.getDocumentNumber(), result.getBody().getData().getDocumentNumber());
        assertTrue(result.getBody().getData().isHasYanki());
    }
    @Test
    public void associateYanki_InvalidToken() {
        // When
        ResponseEntity<BaseResponse<AssociateYankiResponse>> result =
                bootCoinUserController.associateYanki(yankiRequest, null).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void associateYanki_Error() {
        // Given
        String errorMessage = "Association failed";
        when(bootCoinUserService.associateYanki(any(AssociateYankiRequest.class)))
                .thenReturn(Single.error(new RuntimeException(errorMessage)));
        // When
        ResponseEntity<BaseResponse<AssociateYankiResponse>> result =
                bootCoinUserController.associateYanki(yankiRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getBody().getStatus());
        assertEquals(errorMessage, result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void associateBankAccount_Success() {
        // Given
        when(bootCoinUserService.associateBankAccount(any(AssociateBankAccountRequest.class)))
                .thenReturn(Single.just(bankAccountResponse));
        // When
        ResponseEntity<BaseResponse<AssociateBankAccountResponse>> result =
                bootCoinUserController.associateBankAccount(bankAccountRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Bank account associated successfully", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(bankAccountResponse.getDocumentNumber(), result.getBody().getData().getDocumentNumber());
        assertEquals(bankAccountResponse.getBankAccountId(), result.getBody().getData().getBankAccountId());
    }
    @Test
    public void associateBankAccount_InvalidToken() {
        // When
        ResponseEntity<BaseResponse<AssociateBankAccountResponse>> result =
                bootCoinUserController.associateBankAccount(bankAccountRequest, null).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(401, result.getBody().getStatus());
        assertEquals("Missing or invalid token", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void associateBankAccount_Error() {
        // Given
        String errorMessage = "Association failed";
        when(bootCoinUserService.associateBankAccount(any(AssociateBankAccountRequest.class)))
                .thenReturn(Single.error(new RuntimeException(errorMessage)));
        // When
        ResponseEntity<BaseResponse<AssociateBankAccountResponse>> result =
                bootCoinUserController.associateBankAccount(bankAccountRequest, validToken).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getBody().getStatus());
        assertEquals(errorMessage, result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
}