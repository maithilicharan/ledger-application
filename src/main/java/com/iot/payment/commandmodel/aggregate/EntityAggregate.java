package com.iot.payment.commandmodel.aggregate;

import com.iot.payment.coreapi.commands.CreateEntityCommand;
import com.iot.payment.coreapi.commands.ModifyPostingCommand;
import com.iot.payment.coreapi.commands.TransferCommand;
import com.iot.payment.coreapi.commands.UpdateAccountStatusCommand;
import com.iot.payment.coreapi.events.EntityCreatedEvent;
import com.iot.payment.coreapi.events.ModifyPostingEvent;
import com.iot.payment.coreapi.events.TransferEvent;
import com.iot.payment.coreapi.events.UpdateAccountStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate(snapshotTriggerDefinition = "entityAggregateSnapshotTriggerDefinition")
@Slf4j
public class EntityAggregate {

    @AggregateIdentifier
    private String entityId;

    @CommandHandler
    public EntityAggregate(CreateEntityCommand command) {
        apply(new EntityCreatedEvent(command.entityId()));
    }

    protected EntityAggregate() {
        log.debug("default constructor invoked by Axon framework");
    }

    @CommandHandler
    public void handle(TransferCommand command) {
        if (this.entityId.equals(command.entityId())) {
            apply(new TransferEvent(command.entityId(), command.sourceEntityId(), command.destinationEntityId(), command.amount()));
        } else {
            throw new IllegalStateException("Transfer can only be initiated by the source entity");
        }

    }

    @CommandHandler
    public void handle(UpdateAccountStatusCommand command) {
        if (this.entityId.equals(command.entityId())) {
            apply(new UpdateAccountStatusEvent(command.entityId(), command.accountId(), command.accountState()));
        } else {
            throw new IllegalStateException("Update account status can only be initiated by the entity");
        }
    }

    @CommandHandler
    public void handle(ModifyPostingCommand command) {
        if (this.entityId.equals(command.entityId())) {
            apply(new ModifyPostingEvent(command.entityId(), command.sourceWalletId(), command.destinationWalletId(), command.postingId(), command.newAmount(), command.newState()));
        } else {
            throw new IllegalStateException("Modify posting can only be initiated by the entity");
        }
    }

    @EventSourcingHandler
    public void on(EntityCreatedEvent event) {
        this.entityId = event.getEntityId();
    }
}
