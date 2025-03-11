package com.bank.bootcoinservice.dto.associateaccount;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AssociateBankAccountRequest {
    private String documentNumber;
    private String bankAccountId;
}
