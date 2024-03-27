package com.iot.payment.querymodel;

import com.iot.payment.coreapi.events.EntityCreatedEvent;
import com.iot.payment.coreapi.events.ModifyPostingEvent;
import com.iot.payment.coreapi.events.TransferEvent;
import com.iot.payment.coreapi.events.UpdateAccountStatusEvent;

public interface EntityEventHandler {
    void on(EntityCreatedEvent event);

    void on(TransferEvent event);

    void on(UpdateAccountStatusEvent event);

    void on(ModifyPostingEvent event);

}
