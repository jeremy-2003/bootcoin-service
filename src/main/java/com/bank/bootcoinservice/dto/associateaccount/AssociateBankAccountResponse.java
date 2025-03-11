package com.bank.bootcoinservice.dto.associateaccount;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssociateBankAccountResponse {
    private String documentNumber;
    private String bankAccountId;
}
