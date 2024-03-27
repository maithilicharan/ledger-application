package com.iot.payment.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

//@Value
//@Builder
//@AllArgsConstructor
//@EqualsAndHashCode
public record CreateEntityCommand(@TargetAggregateIdentifier String entityId) {

}
