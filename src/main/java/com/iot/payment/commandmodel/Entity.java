package com.iot.payment.commandmodel;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Entity {
    private String id;
    private List<Account> accounts;
}