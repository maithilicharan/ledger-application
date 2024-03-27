package com.iot.payment.coreapi.commands;

import com.iot.payment.commandmodel.PostingState;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record ModifyPostingCommand(@TargetAggregateIdentifier String entityId, String sourceWalletId, String destinationWalletId, String postingId,
                                   BigDecimal newAmount, PostingState newState) {

}
