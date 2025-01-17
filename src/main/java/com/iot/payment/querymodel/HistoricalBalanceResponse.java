package com.iot.payment.querymodel;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalBalanceResponse {
    private String walletId;
    private BigDecimal balance;
    private LocalDateTime localDate;
}
