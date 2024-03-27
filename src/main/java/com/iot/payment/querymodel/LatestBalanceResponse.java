package com.iot.payment.querymodel;


import com.iot.payment.commandmodel.Entity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LatestBalanceResponse {
    private String walletId;
    private BigDecimal balance;
    private Entity entity;

    public LatestBalanceResponse(Entity entity) {
        this.entity = entity;
    }


}
