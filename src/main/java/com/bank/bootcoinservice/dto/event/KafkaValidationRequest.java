package com.bank.bootcoinservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaValidationRequest {
    private String eventId;
    private String documentNumber;
    private String phoneNumber;
    private String bankAccountId;
}
