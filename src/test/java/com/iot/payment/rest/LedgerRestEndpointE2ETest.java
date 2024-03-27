package com.iot.payment.rest;

import com.iot.payment.LedgerApplication;
import com.iot.payment.querymodel.HistoricalBalanceResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@SpringBootTest(classes = LedgerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LedgerRestEndpointE2ETest {
    @LocalServerPort
    private int port;

    private static ReactorClientHttpConnector httpConnector() {
        HttpClient httpClient = HttpClient.create()
                .wiretap(true);
        return new ReactorClientHttpConnector(httpClient);
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void historicalBalanceShouldReturnHistoricalBalanceResponse() {

        WebClient client = WebClient.builder()
                .clientConnector(httpConnector())
                .build();
        String entityId = UUID.randomUUID()
                .toString();
        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/entity/" + entityId)))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();

        transferBalances(client, entityId);
        StepVerifier.create(retrieveListResponse(client.get()
                        .uri("http://localhost:" + port + "/historical-balance")))
                .expectNextMatches(list -> 2 == list.size()
                        && list.get(0).getLocalDate().isBefore(LocalDateTime.now()))
                .verifyComplete();
    }

    private void transferBalances(WebClient client, String entityId) {
        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/transfer/entity/" + entityId + "/source-id/FiatCurrencySourceWalletId/destination-id/FiatCurrencyDestinationWalletId/amount/100")))
                .verifyComplete();
    }

    private Mono<String> retrieveResponse(WebClient.RequestBodySpec spec) {
        return spec.retrieve()
                .bodyToMono(String.class);
    }

    private Mono<LedgerRestEndpointE2ETest.ResponseList> retrieveListResponse(WebClient.RequestHeadersSpec<?> spec) {
        return spec.accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(LedgerRestEndpointE2ETest.ResponseList.class);
    }

    private static class ResponseList extends ArrayList<HistoricalBalanceResponse> {

        private ResponseList() {
            super();
        }
    }
}
