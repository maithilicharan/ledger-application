package com.iot.payment.service;

import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.PostingState;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerService {
    void modifyPosting(String entityId, String sourceWalletId, String destinationWalletId, String postingId, BigDecimal newAmount, PostingState newState);

    void changeAccountState(String entityId, String accountId, AccountState newState);

    List<String> transfer(String entityId, List<TransferRequest> requests);
}
