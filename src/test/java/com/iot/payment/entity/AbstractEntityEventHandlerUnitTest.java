package com.iot.payment.entity;

import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.Entity;
import com.iot.payment.commandmodel.PostingState;
import com.iot.payment.commandmodel.Wallet;
import com.iot.payment.coreapi.events.BalanceUpdatedEvent;
import com.iot.payment.coreapi.events.EntityCreatedEvent;
import com.iot.payment.coreapi.events.ModifyPostingEvent;
import com.iot.payment.coreapi.events.MovementUpdatedEvent;
import com.iot.payment.coreapi.events.TransferEvent;
import com.iot.payment.coreapi.events.UpdateAccountStatusEvent;
import com.iot.payment.querymodel.EntityEventHandler;
import com.iot.payment.repository.LedgerInMemoryRepository;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public abstract class AbstractEntityEventHandlerUnitTest {
    private static final String ENTITY = UUID.randomUUID()
            .toString();
    private static final String ENTITY_SOURCE_ID = UUID.randomUUID()
            .toString();
    private static final String ENTITY_DESTINATION_ID = UUID.randomUUID()
            .toString();
    protected QueryUpdateEmitter emitter = mock(QueryUpdateEmitter.class);
    protected LedgerInMemoryRepository ledgerRepository = new LedgerInMemoryRepository();
    EventBus eventBus = Mockito.mock(EventBus.class);
    private EntityEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = getHandler();
    }

    protected abstract EntityEventHandler getHandler();

    @Test
    void givenEntityExist_whenTransferEvent_thenMovementCreatedEvent_thenBalanceUpdatedEvent() {
        // Given
        handler.on(new EntityCreatedEvent(ENTITY));
        TransferEvent transferEvent = new TransferEvent(ENTITY, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, new BigDecimal(100));

        // When
        handler.on(transferEvent);

        // Then
        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);
        Mockito.verify(eventBus, Mockito.times(2)).publish(eventMessageCaptor.capture());

        List<EventMessage> publishedEventMessages = eventMessageCaptor.getAllValues();
        assertInstanceOf(MovementUpdatedEvent.class, publishedEventMessages.get(0).getPayload());
        assertInstanceOf(BalanceUpdatedEvent.class, publishedEventMessages.get(1).getPayload());

        MovementUpdatedEvent movementUpdatedEvent = (MovementUpdatedEvent) publishedEventMessages.get(0).getPayload();
        BalanceUpdatedEvent balanceUpdatedEvent = (BalanceUpdatedEvent) publishedEventMessages.get(1).getPayload();
        assertEquals("FIAT_CURRENCY_SOURCE_1", movementUpdatedEvent.sourceWalletId());
        assertEquals("FIAT_CURRENCY_DESTINATION_1", movementUpdatedEvent.destinationWalletId());
        assertEquals(new BigDecimal(100), movementUpdatedEvent.amount());

        // Add assertions for the second published event
        assertEquals("FIAT_CURRENCY_SOURCE_1", balanceUpdatedEvent.sourceWalletId());
        assertEquals("FIAT_CURRENCY_DESTINATION_1", balanceUpdatedEvent.destinationWalletId());
        assertEquals(new BigDecimal(100), balanceUpdatedEvent.amount());
    }

    @Test
    void givenUpdateAccountStatusEvent_whenOn_thenAccountStateUpdated() {
        // Given
        handler.on(new EntityCreatedEvent(ENTITY));
        UpdateAccountStatusEvent event = new UpdateAccountStatusEvent(ENTITY, "ACCOUNT1", AccountState.CLOSED);


        // When
        handler.on(event);

        // Then
        assertEquals(AccountState.CLOSED, ledgerRepository.getEntities().get(ENTITY).getAccounts().get(0).getState());
    }

    @Test
    void givenUpdateAccountStatusEvent_whenOn_thenAccountStateUpdated_thenTransferEvent_throwsException() {
        // Given
        handler.on(new EntityCreatedEvent(ENTITY));
        UpdateAccountStatusEvent event = new UpdateAccountStatusEvent(ENTITY, "ACCOUNT1", AccountState.CLOSED);

        // When
        handler.on(event);

        // Then
        assertEquals(AccountState.CLOSED, ledgerRepository.getEntities().get(ENTITY).getAccounts().get(0).getState());

        // Call transfer method and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            TransferEvent transferEvent = new TransferEvent(ENTITY, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, new BigDecimal(100));
            handler.on(transferEvent);
        });

        String expectedMessage = "Transactions can only be made to and from wallets of accounts in the OPEN state";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }


    @Test
    void givenTwoTransferEvents_whenOnModifyPostingEvent_thenBalanceShouldUpdated() {
        // Given
        handler.on(new EntityCreatedEvent(ENTITY));
        TransferEvent transferEvent1 = new TransferEvent(ENTITY, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, new BigDecimal(10));
        TransferEvent transferEvent2 = new TransferEvent(ENTITY, ENTITY_SOURCE_ID, ENTITY_DESTINATION_ID, new BigDecimal(20));

        // When
        handler.on(transferEvent1);
        handler.on(transferEvent2);

        // Then
        ArgumentCaptor<EventMessage> eventMessageCaptor = ArgumentCaptor.forClass(EventMessage.class);
        Mockito.verify(eventBus, Mockito.times(4)).publish(eventMessageCaptor.capture());

        List<EventMessage> publishedEventMessages = eventMessageCaptor.getAllValues();

        MovementUpdatedEvent movementUpdatedEventOne = (MovementUpdatedEvent) publishedEventMessages.get(0).getPayload();
        MovementUpdatedEvent movementUpdatedEventTwo = (MovementUpdatedEvent) publishedEventMessages.get(2).getPayload();

        // Retrieve the Entity and the Wallets
        Entity entity = ledgerRepository.getEntities().get(ENTITY);
        Wallet sourceWallet = entity.getAccounts().get(0).getWallets().stream().filter(wallet -> wallet.getId().equals(movementUpdatedEventTwo.sourceWalletId())).findFirst().orElse(null);
        Wallet destinationWallet = entity.getAccounts().get(0).getWallets().stream().filter(wallet -> wallet.getId().equals(movementUpdatedEventTwo.destinationWalletId())).findFirst().orElse(null);

        // Assert that the balances of the Wallets are as expected
        BigDecimal expectedSourceWalletBalance = new BigDecimal(70);
        BigDecimal expectedDestinationWalletBalance = new BigDecimal(130);
        assertEquals(expectedSourceWalletBalance, sourceWallet.getBalance());
        assertEquals(expectedDestinationWalletBalance, destinationWallet.getBalance());

        // Modify the first posting
        ModifyPostingEvent modifyPostingEvent = new ModifyPostingEvent(ENTITY, movementUpdatedEventOne.sourceWalletId(), movementUpdatedEventOne.destinationWalletId(), movementUpdatedEventOne.postingId(), new BigDecimal(15), PostingState.CLEARED);
        handler.on(modifyPostingEvent);

        // Capture the MovementCreatedEvent after the ModifyPostingEvent
        Mockito.verify(eventBus, Mockito.times(6)).publish(eventMessageCaptor.capture());
        MovementUpdatedEvent movementUpdatedEventThree = (MovementUpdatedEvent) eventMessageCaptor.getAllValues().get(2).getPayload();

        // Retrieve the Entity and the Wallets
        entity = ledgerRepository.getEntities().get(ENTITY);
        sourceWallet = entity.getAccounts().get(0).getWallets().stream().filter(wallet -> wallet.getId().equals(movementUpdatedEventThree.sourceWalletId())).findFirst().orElse(null);
        destinationWallet = entity.getAccounts().get(0).getWallets().stream().filter(wallet -> wallet.getId().equals(movementUpdatedEventThree.destinationWalletId())).findFirst().orElse(null);

        // Assert that the balances of the Wallets are as expected
        expectedSourceWalletBalance = new BigDecimal(65);
        expectedDestinationWalletBalance = new BigDecimal(135);
        assertEquals(expectedSourceWalletBalance, sourceWallet.getBalance());
        assertEquals(expectedDestinationWalletBalance, destinationWallet.getBalance());
    }
}
