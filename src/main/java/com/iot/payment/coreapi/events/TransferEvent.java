package com.iot.payment.coreapi.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.AggregateIdentifier;

import java.math.BigDecimal;

@Builder
@Value
@AllArgsConstructor
public class TransferEvent {

    @AggregateIdentifier
    String entityId;
    String sourceWalletId;
    String destinationWalletId;
    BigDecimal amount;

}