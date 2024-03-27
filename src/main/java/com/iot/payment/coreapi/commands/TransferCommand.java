package com.iot.payment.coreapi.commands;


import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record TransferCommand(@TargetAggregateIdentifier String entityId, String sourceEntityId, String destinationEntityId, BigDecimal amount) {
}
