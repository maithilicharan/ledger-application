package com.iot.payment.commandmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class Account {
    private String id;
    private List<Wallet> wallets;
    private AccountState state;

    public void close() {
        this.state = AccountState.CLOSED;
    }

    public void suspend() {
        this.state = AccountState.SUSPENDED;
    }

    public void open() {
        this.state = AccountState.OPEN;
    }

    public void freeze() {
        this.state = AccountState.FROZEN;
    }

}