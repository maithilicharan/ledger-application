package com.iot.payment.entity;

import com.iot.payment.querymodel.EntityEventHandler;
import com.iot.payment.querymodel.InMemoryEntityEventHandler;

public class InMemoryLedgerEventsHandlerUnitTest extends AbstractEntityEventHandlerUnitTest {

    @Override
    protected EntityEventHandler getHandler() {
        return new InMemoryEntityEventHandler(ledgerRepository, emitter, eventBus);
    }
}
