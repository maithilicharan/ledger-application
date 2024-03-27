package com.iot.payment.coreapi.events;

import com.iot.payment.commandmodel.AccountState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Builder
@AllArgsConstructor
@Value
public class UpdateAccountStatusEvent {
    String entityId;
    String accountId;
    AccountState accountState;

}
