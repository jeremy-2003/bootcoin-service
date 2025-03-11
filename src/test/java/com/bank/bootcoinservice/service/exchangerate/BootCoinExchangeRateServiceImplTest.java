package com.bank.bootcoinservice.service.exchangerate;

import com.bank.bootcoinservice.model.exchangerate.BootCoinExchangeRate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BootCoinExchangeRateServiceImplTest {
    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private BootCoinExchangeRateServiceImpl exchangeRateService;
    private BootCoinExchangeRate exchangeRate;
    private String exchangeRateJson;
    private static final String EXCHANGE_RATE_KEY = "bootcoin:exchange_rate";
    @BeforeEach
    void setUp() throws Exception {
        // Initialize test data
        exchangeRate = new BootCoinExchangeRate();
        exchangeRate.setBuyRate(new BigDecimal("3.75"));
        exchangeRate.setSellRate(new BigDecimal("3.85"));
        exchangeRate.setUpdatedAt(LocalDateTime.now());
        // Create JSON representation
        exchangeRateJson = "{\"buyRate\":3.75,\"sellRate\":3.85,\"updatedAt\":\"2023-01-01T12:00:00\"}";
        // Mock the operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    @Test
    void getCurrentExchangeRate_Success() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.just(exchangeRateJson));
        when(objectMapper.readValue(exchangeRateJson, BootCoinExchangeRate.class)).thenReturn(exchangeRate);
        // When
        BootCoinExchangeRate result = exchangeRateService.getCurrentExchangeRate().blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(exchangeRate.getBuyRate(), result.getBuyRate());
        assertEquals(exchangeRate.getSellRate(), result.getSellRate());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).readValue(exchangeRateJson, BootCoinExchangeRate.class);
    }
    @Test
    void getCurrentExchangeRate_NotFound() {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.empty());
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService.getCurrentExchangeRate().blockingGet();
        });
        assertEquals("Exchange rate not found", exception.getMessage());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verifyNoInteractions(objectMapper);
    }
    @Test
    void getCurrentExchangeRate_DeserializationError() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.just(exchangeRateJson));
        when(objectMapper.readValue(exchangeRateJson, BootCoinExchangeRate.class))
                .thenThrow(new RuntimeException("JSON parsing error"));
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService.getCurrentExchangeRate().blockingGet();
        });
        assertTrue(exception.getMessage().contains("Error parsing exchange rate data"));
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).readValue(exchangeRateJson, BootCoinExchangeRate.class);
    }
    private void setupRedisOperations() {
        // Mock the operations - moved from setUp to here so it's only called when needed
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    @Test
    void saveExchangeRateToCache_Success() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.empty());
        when(objectMapper.writeValueAsString(any(BootCoinExchangeRate.class))).thenReturn(exchangeRateJson);
        when(valueOperations.set(eq(EXCHANGE_RATE_KEY), eq(exchangeRateJson))).thenReturn(Mono.just(Boolean.TRUE));
        // When
        BootCoinExchangeRate result = exchangeRateService
                .saveExchangeRateToCache(new BigDecimal("3.75"), new BigDecimal("3.85"))
                .blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("3.75"), result.getBuyRate());
        assertEquals(new BigDecimal("3.85"), result.getSellRate());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).writeValueAsString(any(BootCoinExchangeRate.class));
        verify(valueOperations).set(eq(EXCHANGE_RATE_KEY), eq(exchangeRateJson));
    }
    @Test
    void saveExchangeRateToCache_AlreadyExists() {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.just(exchangeRateJson));
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService
                    .saveExchangeRateToCache(new BigDecimal("3.75"), new BigDecimal("3.85"))
                    .blockingGet();
        });
        assertEquals("Exchange rate already exists, use update instead", exception.getMessage());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verifyNoMoreInteractions(objectMapper);
        verify(valueOperations, never()).set(anyString(), anyString());
    }
    @Test
    void saveExchangeRateToCache_SerializationError() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.empty());
        when(objectMapper.writeValueAsString(any(BootCoinExchangeRate.class)))
                .thenThrow(new RuntimeException("JSON serialization error"));
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService
                    .saveExchangeRateToCache(new BigDecimal("3.75"), new BigDecimal("3.85"))
                    .blockingGet();
        });
        assertTrue(exception.getMessage().contains("JSON serialization error"));
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).writeValueAsString(any(BootCoinExchangeRate.class));
        verify(valueOperations, never()).set(anyString(), anyString());
    }
    @Test
    void updateExchangeRate_Success() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.just(exchangeRateJson));
        when(objectMapper.readValue(exchangeRateJson, BootCoinExchangeRate.class)).thenReturn(exchangeRate);
        when(objectMapper.writeValueAsString(any(BootCoinExchangeRate.class))).thenReturn(exchangeRateJson);
        when(valueOperations.set(eq(EXCHANGE_RATE_KEY), eq(exchangeRateJson))).thenReturn(Mono.just(Boolean.TRUE));
        // When
        BootCoinExchangeRate result = exchangeRateService
                .updateExchangeRate(new BigDecimal("3.90"), new BigDecimal("4.00"))
                .blockingGet();
        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("3.90"), result.getBuyRate());
        assertEquals(new BigDecimal("4.00"), result.getSellRate());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).readValue(exchangeRateJson, BootCoinExchangeRate.class);
        verify(objectMapper).writeValueAsString(any(BootCoinExchangeRate.class));
        verify(valueOperations).set(eq(EXCHANGE_RATE_KEY), eq(exchangeRateJson));
    }
    @Test
    void updateExchangeRate_NotFound() {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.empty());
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService
                    .updateExchangeRate(new BigDecimal("3.90"), new BigDecimal("4.00"))
                    .blockingGet();
        });
        assertEquals("Exchange rate does not exist, cannot update", exception.getMessage());
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verifyNoInteractions(objectMapper);
    }
    @Test
    void updateExchangeRate_DeserializationError() throws Exception {
        // Given
        when(valueOperations.get(EXCHANGE_RATE_KEY)).thenReturn(Mono.just(exchangeRateJson));
        when(objectMapper.readValue(exchangeRateJson, BootCoinExchangeRate.class))
                .thenThrow(new RuntimeException("JSON parsing error"));
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            exchangeRateService
                    .updateExchangeRate(new BigDecimal("3.90"), new BigDecimal("4.00"))
                    .blockingGet();
        });
        assertTrue(exception.getMessage().contains("Error parsing existing exchange rate data"));
        verify(valueOperations).get(EXCHANGE_RATE_KEY);
        verify(objectMapper).readValue(exchangeRateJson, BootCoinExchangeRate.class);
        verify(objectMapper, never()).writeValueAsString(any());
        verify(valueOperations, never()).set(anyString(), anyString());
    }
}
