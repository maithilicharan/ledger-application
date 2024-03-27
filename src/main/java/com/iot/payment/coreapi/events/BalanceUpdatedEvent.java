package com.iot.payment.coreapi.events;

import java.math.BigDecimal;

public record BalanceUpdatedEvent(String sourceWalletId, String destinationWalletId, BigDecimal amount) {
}