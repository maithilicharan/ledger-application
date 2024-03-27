package com.iot.payment.coreapi.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@AllArgsConstructor
public class EntityCreatedEvent {
    String entityId;
}
