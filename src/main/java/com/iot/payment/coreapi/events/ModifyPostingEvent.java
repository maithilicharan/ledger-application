package com.iot.payment.coreapi.events;

import com.iot.payment.commandmodel.PostingState;

import java.math.BigDecimal;

public record ModifyPostingEvent(String entityId, String sourceWalletId, String destinationWalletId,
                                 String postingId,
                                 BigDecimal newAmount,
                                 PostingState newState) {
}
