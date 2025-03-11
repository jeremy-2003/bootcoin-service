package com.bank.bootcoinservice.model.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Document(collection = "bootcoin_user")
public class BootCoinUser {
    @Id
    private String id;
    private String documentNumber;
    private String phoneNumber;
    private String email;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean hasYanki;
    private String bankAccountId;
}
