package com.bank.bootcoinservice.dto.associateyanki;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssociateYankiResponse {
    private String documentNumber;
    private boolean hasYanki;
}
