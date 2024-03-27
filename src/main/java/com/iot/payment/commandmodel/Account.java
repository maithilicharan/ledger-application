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

}