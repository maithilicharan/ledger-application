package com.iot.payment.querymodel;

import com.iot.payment.commandmodel.Entity;
import com.iot.payment.coreapi.queries.FindHistoricalBalanceOfWallet;
import lombok.AllArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EntityQueryService {

    private final QueryGateway queryGateway;

    public CompletableFuture<List<HistoricalBalanceResponse>> findHistoricalBalanceOfWallet() {
        return queryGateway.query(new FindHistoricalBalanceOfWallet(LocalDateTime.now()), ResponseTypes.multipleInstancesOf(HistoricalBalanceResponse.class))
                .thenApply(r -> r.stream()
                        .map(entity -> HistoricalBalanceResponse.builder()
                                .walletId(entity.getWalletId())
                                .balance(entity.getBalance())
                                .localDate(LocalDateTime.now())
                                .build())
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<List<LatestBalanceResponse>> findLatestBalance() {
        return queryGateway.query(new FindHistoricalBalanceOfWallet(LocalDateTime.now()), ResponseTypes.multipleInstancesOf(Entity.class))
                .thenApply(r -> r.stream()
                        .map(LatestBalanceResponse::new)
                        .collect(Collectors.toList()));
    }
}
