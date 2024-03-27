package com.iot.payment.repository;

import com.iot.payment.commandmodel.Entity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Repository
public class LedgerInMemoryRepository {
    private Map<String, Entity> entities = new ConcurrentHashMap<>();
}