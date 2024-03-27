package com.iot.payment.commandmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
public class BalanceHistory {
    private BigDecimal balance;
    private LocalDateTime timestamp;

}