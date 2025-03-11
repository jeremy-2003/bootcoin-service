package com.bank.bootcoinservice.dto.bootcoin;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BootCoinUserRegistrationRequest {
    private String documentNumber;
    private String phoneNumber;
    private String email;
    private boolean associateYanki;
    private String bankAccountId;
}
