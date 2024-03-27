package com.iot.payment.commandmodel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Wallet {
    @Builder.Default
    private List<BalanceHistory> balanceHistory = new ArrayList<>();
    private String id;
    private BigDecimal balance;
    private Account account;
    private AssetType assetType;
    @Builder.Default
    private List<Posting> postings = new ArrayList<>();

    public void addBalanceHistory(BigDecimal balance, LocalDateTime timestamp) {
        balanceHistory.add(new BalanceHistory(balance, timestamp));
    }

    public BigDecimal getBalanceAt(LocalDateTime timestamp) {
        for (BalanceHistory history : balanceHistory) {
            if (history.getTimestamp().equals(timestamp)) {
                return history.getBalance();
            }
        }
        return null;
    }

}

