package com.iot.payment.coreapi.commands;

import com.iot.payment.commandmodel.AccountState;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record UpdateAccountStatusCommand(@TargetAggregateIdentifier String entityId, String accountId, AccountState accountState) {
}
