package com.iot.payment.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferRequest {
    private String sourceWalletId;
    private String destinationWalletId;
    private BigDecimal amount;
}