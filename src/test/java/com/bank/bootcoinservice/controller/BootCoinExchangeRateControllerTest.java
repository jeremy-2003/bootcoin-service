package com.bank.bootcoinservice.controller;

import com.bank.bootcoinservice.dto.BaseResponse;
import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import com.bank.bootcoinservice.service.exchangerate.BootCoinExchangeRateService;
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
public class BootCoinExchangeRateControllerTest {
    @Mock
    private BootCoinExchangeRateService exchangeRateService;
    @InjectMocks
    private BootCoinExchangeRateController exchangeRateController;
    private BootCoinExchangeRate exchangeRate;
    private LocalDateTime now;
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        exchangeRate = new BootCoinExchangeRate();
        exchangeRate.setBuyRate(new BigDecimal("3.75"));
        exchangeRate.setSellRate(new BigDecimal("3.85"));
        exchangeRate.setUpdatedAt(now);
    }
    @Test
    public void getExchangeRate_Success() {
        // Given
        when(exchangeRateService.getCurrentExchangeRate())
                .thenReturn(Single.just(exchangeRate));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.getExchangeRate().blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Exchange rate retrieved", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(exchangeRate.getBuyRate(), result.getBody().getData().getBuyRate());
        assertEquals(exchangeRate.getSellRate(), result.getBody().getData().getSellRate());
        assertEquals(exchangeRate.getUpdatedAt(), result.getBody().getData().getUpdatedAt());
    }
    @Test
    public void getExchangeRate_NotFound() {
        // Given
        when(exchangeRateService.getCurrentExchangeRate())
                .thenReturn(Single.error(new RuntimeException("Exchange rate not found")));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.getExchangeRate().blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getBody().getStatus());
        assertEquals("No exchange rate found", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void saveExchangeRate_Success() {
        // Given
        when(exchangeRateService.saveExchangeRateToCache(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(Single.just(exchangeRate));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.saveExchangeRate(exchangeRate).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(HttpStatus.CREATED.value(), result.getBody().getStatus());
        assertEquals("Exchange rate created", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(exchangeRate.getBuyRate(), result.getBody().getData().getBuyRate());
        assertEquals(exchangeRate.getSellRate(), result.getBody().getData().getSellRate());
    }
    @Test
    public void saveExchangeRate_AlreadyExists() {
        // Given
        when(exchangeRateService.saveExchangeRateToCache(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(Single.error(new RuntimeException("Exchange rate already exists")));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.saveExchangeRate(exchangeRate).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getBody().getStatus());
        assertEquals("Exchange already exists", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
    @Test
    public void updateExchangeRate_Success() {
        // Given
        BootCoinExchangeRate updatedRate = new BootCoinExchangeRate();
        updatedRate.setBuyRate(new BigDecimal("3.80"));
        updatedRate.setSellRate(new BigDecimal("3.90"));
        updatedRate.setUpdatedAt(now);
        when(exchangeRateService.updateExchangeRate(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(Single.just(updatedRate));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.updateExchangeRate(exchangeRate).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(HttpStatus.OK.value(), result.getBody().getStatus());
        assertEquals("Exchange rate updated", result.getBody().getMessage());
        assertNotNull(result.getBody().getData());
        assertEquals(updatedRate.getBuyRate(), result.getBody().getData().getBuyRate());
        assertEquals(updatedRate.getSellRate(), result.getBody().getData().getSellRate());
    }
    @Test
    public void updateExchangeRate_NotFound() {
        // Given
        when(exchangeRateService.updateExchangeRate(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(Single.error(new RuntimeException("Exchange rate not found")));
        // When
        ResponseEntity<BaseResponse<BootCoinExchangeRate>> result =
                exchangeRateController.updateExchangeRate(exchangeRate).blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getBody().getStatus());
        assertEquals("Exchange rate does not exist, cannot update", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
}
