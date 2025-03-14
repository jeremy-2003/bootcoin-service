package com.bank.bootcoinservice.dto;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseResponse<T> {
    private int status;
    private String message;
    private T data;
}
