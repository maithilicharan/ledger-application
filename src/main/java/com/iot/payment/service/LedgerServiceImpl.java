package com.iot.payment.service;

import com.iot.payment.commandmodel.Account;
import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.Entity;
import com.iot.payment.commandmodel.Posting;
import com.iot.payment.commandmodel.PostingState;
import com.iot.payment.commandmodel.Wallet;
import com.iot.payment.repository.LedgerInMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;


@Service("ledgerService")
@Slf4j
public class LedgerServiceImpl implements LedgerService {
    private final LedgerInMemoryRepository ledgerRepository;
    private final Map<String, BigDecimal> initialBalances;

    public LedgerServiceImpl(LedgerInMemoryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
        this.initialBalances = new HashMap<>();
    }

    private static void updateBalances(Wallet sourceWallet, BigDecimal difference, Wallet destinationWallet) {
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(difference));
        sourceWallet.addBalanceHistory(sourceWallet.getBalance(), LocalDateTime.now());

        destinationWallet.setBalance(destinationWallet.getBalance().add(difference));
        destinationWallet.addBalanceHistory(destinationWallet.getBalance(), LocalDateTime.now());
    }

    @Override
    public void modifyPosting(String entityId, String sourceWalletId, String destinationWalletId, String postingId, BigDecimal newAmount, PostingState newState) {
        Entity entity = ledgerRepository.getEntities().get(entityId);
        if (entity == null) {
            throw new NoSuchElementException("Entity not found");
        }

        Wallet sourceWallet = findWallet(entity, sourceWalletId);
        Wallet destinationWallet = findWallet(entity, destinationWalletId);


        Posting sourcePosting = sourceWallet.getPostings().stream()
                .filter(p -> p.getId().equals(postingId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Posting not found"));

        Posting destinationPosting = destinationWallet.getPostings().stream()
                .filter(p -> p.getId().equals(postingId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Posting not found"));

        if (sourcePosting.getState() == PostingState.FAILED || destinationPosting.getState() == PostingState.FAILED) {
            throw new IllegalStateException("Cannot modify a failed posting");
        }

        if (sourcePosting.getState() == PostingState.PENDING || destinationPosting.getState() == PostingState.PENDING) {
            throw new IllegalStateException("Cannot modify a pending posting");
        }

        BigDecimal oldAmount = sourcePosting.getAmount();
        sourcePosting.setAmount(newAmount.negate());
        destinationPosting.setAmount(newAmount);
        sourcePosting.setState(newState);
        destinationPosting.setState(newState);

        // Adjust the wallets' balances only if the posting is cleared
        if (newState == PostingState.CLEARED) {
            log.info("\nOld amount: " + oldAmount + "\nNew amount: " + newAmount);
            BigDecimal difference = newAmount.add(oldAmount);
            updateBalances(sourceWallet, difference, destinationWallet);
            log.info("\nDifference: " + difference + "\nNew balance of source wallet: " + sourceWallet.getBalance() + "\ndestination wallet: " + destinationWallet.getBalance());
        }
    }

    public void changeAccountState(String entityId, String accountId, AccountState newState) {
        Entity entity = ledgerRepository.getEntities().get(entityId);
        if (entity == null) {
            throw new NoSuchElementException("Entity not found");
        }

        Account account = findAccount(entity, accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found");
        }

        account.setState(newState);
    }

    private Account findAccount(Entity entity, String accountId) {
        for (Account account : entity.getAccounts()) {
            if (account.getId().equals(accountId)) {
                return account;
            }
        }
        return null;
    }


    public List<String> transfer(String entityId, List<TransferRequest> requests) {
        Entity entity = ledgerRepository.getEntities().get(entityId);
        if (entity == null) {
            throw new NoSuchElementException("Entity not found");
        }

        List<String> postingIds = new ArrayList<>();
        try {
            for (TransferRequest request : requests) {
                Wallet sourceWallet = findWallet(entity, request.getSourceWalletId());
                Wallet destinationWallet = findWallet(entity, request.getDestinationWalletId());

                if (sourceWallet.getAccount().getState() != AccountState.OPEN || destinationWallet.getAccount().getState() != AccountState.OPEN) {
                    throw new IllegalStateException("Transactions can only be made to and from wallets of accounts in the OPEN state");
                }

                if (!initialBalances.containsKey(sourceWallet.getId())) {
                    initialBalances.put(sourceWallet.getId(), sourceWallet.getBalance());
                }

                if (!initialBalances.containsKey(destinationWallet.getId())) {
                    initialBalances.put(destinationWallet.getId(), destinationWallet.getBalance());
                }

                if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
                    throw new IllegalArgumentException("Insufficient balance in source wallet");
                }
                updateBalances(sourceWallet, request.getAmount(), destinationWallet);

                // Create postings
                String postingId = UUID.randomUUID().toString();
                Posting sourcePosting = Posting.builder().id(postingId).amount(request.getAmount().negate()).state(PostingState.CLEARED).build();
                Posting destinationPosting = Posting.builder().id(postingId).amount(request.getAmount()).state(PostingState.CLEARED).build();

                // Add postings to wallets
                sourceWallet.getPostings().add(sourcePosting);
                destinationWallet.getPostings().add(destinationPosting);
                postingIds.add(postingId);

            }
        } catch (Exception e) {
            rollback(entity);
            throw e;
        }
        return postingIds;
    }

    private void rollback(Entity entity) {
        for (Map.Entry<String, BigDecimal> entry : initialBalances.entrySet()) {
            Wallet wallet = findWallet(entity, entry.getKey());
            wallet.setBalance(entry.getValue());
        }
    }

    private Wallet findWallet(Entity entity, String walletId) {
        for (Account account : entity.getAccounts()) {
            for (Wallet wallet : account.getWallets()) {
                if (wallet.getId().equals(walletId)) {
                    wallet.setAccount(account);
                    return wallet;
                }
            }
        }
        throw new NoSuchElementException("Wallet not found");
    }
}