package com.iot.payment.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CreateEntityCommand(@TargetAggregateIdentifier String entityId) {

}
