package com.iot.payment.coreapi.events;

import java.math.BigDecimal;

public record MovementUpdatedEvent(String movementId, String sourceWalletId, String destinationWalletId,
                                   BigDecimal amount, String postingId) {

}
