package com.iot.payment.querymodel;

import com.iot.payment.commandmodel.Account;
import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.AssetType;
import com.iot.payment.commandmodel.Entity;
import com.iot.payment.commandmodel.Wallet;
import com.iot.payment.coreapi.events.BalanceUpdatedEvent;
import com.iot.payment.coreapi.events.EntityCreatedEvent;
import com.iot.payment.coreapi.events.ModifyPostingEvent;
import com.iot.payment.coreapi.events.MovementUpdatedEvent;
import com.iot.payment.coreapi.events.TransferEvent;
import com.iot.payment.coreapi.events.UpdateAccountStatusEvent;
import com.iot.payment.coreapi.queries.FindHistoricalBalanceOfWallet;
import com.iot.payment.repository.LedgerInMemoryRepository;
import com.iot.payment.service.LedgerService;
import com.iot.payment.service.LedgerServiceImpl;
import com.iot.payment.service.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@ProcessingGroup("entities")
public class InMemoryEntityEventHandler implements EntityEventHandler {

    private final static String FIAT_CURRENCY_SOURCE_WALLET_ID = AssetType.FIAT_CURRENCY.name() + "_SOURCE_1";
    private final static String FIAT_CURRENCY_DESTINATION_WALLET_ID = AssetType.FIAT_CURRENCY.name() + "_DESTINATION_1";
    private final LedgerInMemoryRepository ledgerRepository;
    private final QueryUpdateEmitter emitter;
    private final EventBus eventBus;
    private final LedgerService ledgerService;
    Map<String, Entity> entities = new HashMap<>();

    public InMemoryEntityEventHandler(LedgerInMemoryRepository ledgerRepository, QueryUpdateEmitter emitter, EventBus eventBus) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerService = new LedgerServiceImpl(ledgerRepository);
        this.emitter = emitter;
        this.eventBus = eventBus;
    }

    @EventHandler
    public void on(EntityCreatedEvent event) {

        //Assume we are maintaining only on Wallet per entity of asset type FIAT_CURRENCY
        Wallet sourceWallet = Wallet.builder().id(FIAT_CURRENCY_SOURCE_WALLET_ID).assetType(AssetType.FIAT_CURRENCY).balance(new BigDecimal(100)).build();
        Wallet destinationWallet = Wallet.builder().id(FIAT_CURRENCY_DESTINATION_WALLET_ID).assetType(AssetType.FIAT_CURRENCY).balance(new BigDecimal(100)).build();
        //Assume we are maintaining only one account per entity
        List<Account> accounts = new ArrayList<>();
        Account account = Account.builder().id("ACCOUNT1").wallets(List.of(sourceWallet, destinationWallet)).state(AccountState.OPEN).build();
        accounts.add(account);
        Entity entity = Entity.builder().id(event.getEntityId()).accounts(accounts).build();
        entities.put(event.getEntityId(), entity);
        ledgerRepository.setEntities(entities);
    }

    @EventHandler
    public void on(TransferEvent event) {
        //TODO create Entity registry along with aggregate root then fetch the source and destination wallet ids to build the TransferRequest.
        TransferRequest transferRequest = TransferRequest.builder().sourceWalletId(FIAT_CURRENCY_SOURCE_WALLET_ID).destinationWalletId(FIAT_CURRENCY_DESTINATION_WALLET_ID).amount(event.getAmount()).build();
        List<TransferRequest> transferRequests = new ArrayList<>();
        transferRequests.add(transferRequest);
        List<String> postingIds = ledgerService.transfer(event.getEntityId(), transferRequests);
        log.info("Posting Ids: " + postingIds);


        publishMovementUpdateEvent(event.getAmount(), postingIds.get(0));
        publishBalanceUpdatedEvent(event.getAmount());

    }

    private void publishMovementUpdateEvent(BigDecimal amount, String postingIds) {
        MovementUpdatedEvent movementUpdatedEvent = new MovementUpdatedEvent(
                UUID.randomUUID().toString(),
                FIAT_CURRENCY_SOURCE_WALLET_ID,
                FIAT_CURRENCY_DESTINATION_WALLET_ID,
                amount,
                postingIds);

        EventMessage<MovementUpdatedEvent> movementCreatedEventEventMessage = GenericEventMessage.asEventMessage(movementUpdatedEvent);

        eventBus.publish(movementCreatedEventEventMessage);
    }

    private void publishBalanceUpdatedEvent(BigDecimal amount) {
        BalanceUpdatedEvent balanceUpdatedEvent = new BalanceUpdatedEvent(
                InMemoryEntityEventHandler.FIAT_CURRENCY_SOURCE_WALLET_ID, InMemoryEntityEventHandler.FIAT_CURRENCY_DESTINATION_WALLET_ID, amount);


        EventMessage<BalanceUpdatedEvent> balanceUpdatedEventMessage = GenericEventMessage.asEventMessage(balanceUpdatedEvent);

        eventBus.publish(balanceUpdatedEventMessage);
    }

    @EventHandler
    public void on(UpdateAccountStatusEvent event) {
        ledgerService.changeAccountState(event.getEntityId(), event.getAccountId(), event.getAccountState());
    }

    @EventHandler
    public void on(ModifyPostingEvent event) {

        ledgerService.modifyPosting(event.entityId(), event.sourceWalletId(), event.destinationWalletId(), event.postingId(), event.newAmount(), event.newState());

        publishMovementUpdateEvent(event.newAmount(), event.postingId());
        publishBalanceUpdatedEvent(event.newAmount());

    }

    @QueryHandler
    public List<HistoricalBalanceResponse> handle(FindHistoricalBalanceOfWallet query) {
        LocalDateTime queryDate = query.localDateTime();

        return entities.values().stream()
                .flatMap(entity -> entity.getAccounts().stream())
                .flatMap(account -> account.getWallets().stream())
                .map(wallet -> HistoricalBalanceResponse.builder()
                        .walletId(wallet.getId())
                        .balance(wallet.getBalanceAt(queryDate))
                        .localDate(queryDate)
                        .build())
                .collect(Collectors.toList());
    }

}
