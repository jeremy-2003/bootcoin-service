package com.bank.bootcoinservice.model.exchangerate;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BootCoinExchangeRate {
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDateTime updatedAt;
}