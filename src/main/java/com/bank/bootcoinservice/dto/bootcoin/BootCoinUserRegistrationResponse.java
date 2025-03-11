package com.bank.bootcoinservice.dto.bootcoin;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BootCoinUserRegistrationResponse {
    private String userId;
    private boolean hasYanki;
    private String bankAccountId;
}
