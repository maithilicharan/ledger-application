package com.iot.payment.service;

import com.iot.payment.commandmodel.Account;
import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.BalanceHistory;
import com.iot.payment.commandmodel.Entity;
import com.iot.payment.commandmodel.Posting;
import com.iot.payment.commandmodel.PostingState;
import com.iot.payment.commandmodel.Wallet;
import com.iot.payment.repository.LedgerInMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LedgerServiceImplUnitTest {
    private LedgerInMemoryRepository ledgerRepository;
    private LedgerService ledgerService;

    @BeforeEach
    public void setup() {
        ledgerRepository = Mockito.mock(LedgerInMemoryRepository.class);
        ledgerService = new LedgerServiceImpl(ledgerRepository);
    }

    @Test
    public void transferShouldSucceedWhenBalanceIsSufficient() {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("100")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();
        destinationWallet.setBalance(new BigDecimal("50"));

        Entity entity = Entity.builder().accounts(Collections.singletonList(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Collections.singletonList(TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build());
        ledgerService.transfer("entityId", requests);

        assertEquals(new BigDecimal("50"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("100"), destinationWallet.getBalance());
    }

    @Test
    public void transferShouldFailWhenBalanceIsInsufficient() {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("40")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();

        Entity entity = Entity.builder().accounts(Collections.singletonList(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Collections.singletonList(TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build());
        assertThrows(IllegalArgumentException.class, () -> ledgerService.transfer("entityId", requests));
    }

    @Test
    public void transferShouldFailWhenEntityNotFound() {
        Map<String, Entity> entities = new HashMap<>();
        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Collections.singletonList(TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build());
        assertThrows(NoSuchElementException.class, () -> ledgerService.transfer("entityId", requests));
    }

    @Test
    public void transferShouldFailWhenWalletNotFound() {

        Entity entity = Entity.builder().accounts(Collections.singletonList(Account.builder().wallets(Collections.singletonList(Wallet.builder().id("nonExistingWalletId").build())).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Collections.singletonList(TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build());
        assertThrows(NoSuchElementException.class, () -> ledgerService.transfer("entityId", requests));
    }


    @Test
    public void transferShouldRollbackOnFailure() {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("100")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();

        Entity entity = Entity.builder().accounts(Collections.singletonList(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Arrays.asList(
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build(),
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("100")).build()
        );

        assertThrows(IllegalArgumentException.class, () -> ledgerService.transfer("entityId", requests));

        assertEquals(new BigDecimal("100"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("50"), destinationWallet.getBalance());
    }

    @ParameterizedTest
    @EnumSource(AccountState.class)
    public void transferShouldFailWhenAccountIsNotOpen(AccountState state) {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("100")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();
        Entity entity = Entity.builder().id("entityId").accounts(Collections.singletonList(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(state).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        List<TransferRequest> requests = Collections.singletonList(TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build());
        if (state == AccountState.OPEN) {
            ledgerService.transfer("entityId", requests);
            assertEquals(new BigDecimal("50"), sourceWallet.getBalance());
            assertEquals(new BigDecimal("100"), destinationWallet.getBalance());
        } else {
            assertThrows(IllegalStateException.class, () -> ledgerService.transfer("entityId", requests));
        }
    }

    @Test
    public void changeAccountStateShouldSucceedWhenAccountExists() {
        Account account = Account.builder().id("accountId").state(AccountState.OPEN).build();

        Entity entity = Entity.builder().accounts(List.of(account)).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        ledgerService.changeAccountState("entityId", "accountId", AccountState.CLOSED);

        assertEquals(AccountState.CLOSED, account.getState());
    }

    @Test
    public void changeAccountStateShouldFailWhenEntityNotFound() {
        Map<String, Entity> entities = new HashMap<>();
        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        assertThrows(NoSuchElementException.class, () -> ledgerService.changeAccountState("entityId", "accountId", AccountState.CLOSED));
    }

    @Test
    public void changeAccountStateShouldFailWhenAccountNotFound() {

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", Entity.builder().accounts(List.of()).build());

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        assertThrows(NoSuchElementException.class, () -> ledgerService.changeAccountState("entityId", "accountId", AccountState.CLOSED));
    }

    @ParameterizedTest
    @EnumSource(PostingState.class)
    public void modifyPostingShouldHandlePostingStatesCorrectly(PostingState state) {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("100")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();

        Posting sourcePosting = Posting.builder().id("postingId").amount(new BigDecimal("50")).state(state).build();
        Posting destinationPosting = Posting.builder().id("postingId").amount(new BigDecimal("50")).state(state).build();

        sourceWallet.setPostings(Collections.singletonList(sourcePosting));
        destinationWallet.setPostings(Collections.singletonList(destinationPosting));

        Entity entity = Entity.builder().accounts(List.of(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        if (state == PostingState.CLEARED) {
            ledgerService.modifyPosting("entityId", "sourceWalletId", "destinationWalletId", "postingId", new BigDecimal("60"), PostingState.CLEARED);
            assertEquals(new BigDecimal("-60"), sourcePosting.getAmount());
            assertEquals(new BigDecimal("60"), destinationPosting.getAmount());
            assertEquals(PostingState.CLEARED, sourcePosting.getState());
            assertEquals(PostingState.CLEARED, destinationPosting.getState());
            assertEquals(new BigDecimal("-10"), sourceWallet.getBalance());
            assertEquals(new BigDecimal("160"), destinationWallet.getBalance());
        } else {
            assertThrows(IllegalStateException.class, () -> ledgerService.modifyPosting("entityId", "sourceWalletId", "destinationWalletId", "postingId", new BigDecimal("60"), PostingState.CLEARED));
        }
    }

    @Test
    public void shouldHandleTwoTransfersAndOneModifyPosting() {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("200")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();

        Entity entity = Entity.builder().id("entityId")
                .accounts(List.of(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        // Combine two transfer requests into one call
        List<TransferRequest> requests = Arrays.asList(
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build(),
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build()
        );
        List<String> postingIds = ledgerService.transfer("entityId", requests);


        // Modify posting
        ledgerService.modifyPosting("entityId", "sourceWalletId", "destinationWalletId", postingIds.get(0), new BigDecimal("60"), PostingState.CLEARED);

        assertEquals(new BigDecimal("90"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("160"), destinationWallet.getBalance());
    }

    @Test
    public void testTwoTransfersAndOneModifyPostingWithBalanceHistory() {
        Wallet sourceWallet = Wallet.builder().id("sourceWalletId").balance(new BigDecimal("300")).build();
        Wallet destinationWallet = Wallet.builder().id("destinationWalletId").balance(new BigDecimal("50")).build();

        BigDecimal initialSourceBalance = sourceWallet.getBalance();
        BigDecimal initialDestinationBalance = destinationWallet.getBalance();


        Entity entity = Entity.builder().id("entityId")
                .accounts(List.of(Account.builder().wallets(Arrays.asList(sourceWallet, destinationWallet)).state(AccountState.OPEN).build())).build();

        Map<String, Entity> entities = new HashMap<>();
        entities.put("entityId", entity);

        Mockito.when(ledgerRepository.getEntities()).thenReturn(entities);

        // First transfer
        List<TransferRequest> requestsDay1 = Collections.singletonList(
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build()
        );
        List<String> postingIdsDay1 = ledgerService.transfer("entityId", requestsDay1);

        assertEquals(new BigDecimal("250"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("100"), destinationWallet.getBalance());

        // Second transfer
        List<TransferRequest> requestsDay2 = Collections.singletonList(
                TransferRequest.builder().sourceWalletId("sourceWalletId").destinationWalletId("destinationWalletId").amount(new BigDecimal("50")).build()
        );
        ledgerService.transfer("entityId", requestsDay2);

        assertEquals(new BigDecimal("200"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("150"), destinationWallet.getBalance());

        // Modify the first transfer
        ledgerService.modifyPosting("entityId", "sourceWalletId", "destinationWalletId", postingIdsDay1.get(0), new BigDecimal("60"), PostingState.CLEARED);

        assertEquals(new BigDecimal("190"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("160"), destinationWallet.getBalance());

        // Check balance history
        List<BalanceHistory> sourceWalletHistory = sourceWallet.getBalanceHistory();
        assertEquals(3, sourceWalletHistory.size());
        assertEquals(new BigDecimal("250"), sourceWalletHistory.get(0).getBalance());
        assertEquals(new BigDecimal("200"), sourceWalletHistory.get(1).getBalance());
        assertEquals(new BigDecimal("190"), sourceWalletHistory.get(2).getBalance());

        List<BalanceHistory> destinationWalletHistory = destinationWallet.getBalanceHistory();
        assertEquals(3, destinationWalletHistory.size());
        assertEquals(new BigDecimal("100"), destinationWalletHistory.get(0).getBalance());
        assertEquals(new BigDecimal("150"), destinationWalletHistory.get(1).getBalance());
        assertEquals(new BigDecimal("160"), destinationWalletHistory.get(2).getBalance());

        // Check final balances
        BigDecimal finalSourceBalance = sourceWallet.getBalance();
        BigDecimal finalDestinationBalance = destinationWallet.getBalance();

        // 10 is the additional amount transferred in the modify posting
        BigDecimal totalTransferred = requestsDay1.get(0).getAmount().add(requestsDay2.get(0).getAmount()).add(new BigDecimal("10"));

        assertEquals(initialSourceBalance.subtract(totalTransferred), finalSourceBalance);
        assertEquals(initialDestinationBalance.add(totalTransferred), finalDestinationBalance);
    }

}