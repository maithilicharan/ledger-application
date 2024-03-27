package com.iot.payment.commandmodel;

import com.iot.payment.commandmodel.aggregate.EntityAggregate;
import com.iot.payment.coreapi.commands.CreateEntityCommand;
import com.iot.payment.coreapi.commands.ModifyPostingCommand;
import com.iot.payment.coreapi.commands.TransferCommand;
import com.iot.payment.coreapi.commands.UpdateAccountStatusCommand;
import com.iot.payment.coreapi.events.EntityCreatedEvent;
import com.iot.payment.coreapi.events.ModifyPostingEvent;
import com.iot.payment.coreapi.events.TransferEvent;
import com.iot.payment.coreapi.events.UpdateAccountStatusEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

public class EntityAggregateUnitTest {
    private static final String ENTITY_ID = UUID.randomUUID()
            .toString();
    private static final String ENTITY_SOURCE_ID = UUID.randomUUID()
            .toString();
    private static final String ENTITY_DESTINATION_ID = UUID.randomUUID()
            .toString();

    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);
    private FixtureConfiguration<EntityAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(EntityAggregate.class);
    }

    @Test
    void giveNoPriorActivity_whenCreateEntityCommand_thenShouldPublishEntityCreatedEvent() {
        fixture.givenNoPriorActivity()
                .when(new CreateEntityCommand(ENTITY_SOURCE_ID))
                .expectEvents(new EntityCreatedEvent(ENTITY_SOURCE_ID));
    }

    @Test
    void givenEntityCreatedEvent_whenAddProductCommand_thenShouldPublishTransferEvent() {
        fixture.given(new EntityCreatedEvent(ENTITY_ID))
                .when(new TransferCommand(ENTITY_ID, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, AMOUNT))
                .expectEvents(new TransferEvent(ENTITY_ID, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, AMOUNT));
    }

    @Test
    void givenEntityCreatedEvent_whenUpdateAccountStatusCommand_thenShouldPublishUpdateAccountStatusEvent() {
        String accountId = UUID.randomUUID().toString();
        AccountState accountState = AccountState.OPEN;

        fixture.given(new EntityCreatedEvent(ENTITY_ID))
                .when(new UpdateAccountStatusCommand(ENTITY_ID, accountId, accountState))
                .expectEvents(new UpdateAccountStatusEvent(ENTITY_ID, accountId, accountState));
    }


    @Test
    void givenEntityCreatedEvent_whenModifyPostingCommand_thenShouldPublishModifyPostingEvent() {
        String sourceWalletId = UUID.randomUUID().toString();
        String destinationWalletId = UUID.randomUUID().toString();
        String postingId = UUID.randomUUID().toString();
        BigDecimal newAmount = BigDecimal.valueOf(500);
        PostingState newState = PostingState.CLEARED;

        fixture.given(new EntityCreatedEvent(ENTITY_ID))
                .when(new ModifyPostingCommand(ENTITY_ID, sourceWalletId, destinationWalletId, postingId, newAmount, newState))
                .expectEvents(new ModifyPostingEvent(ENTITY_ID, sourceWalletId, destinationWalletId, postingId, newAmount, newState));
    }
}
