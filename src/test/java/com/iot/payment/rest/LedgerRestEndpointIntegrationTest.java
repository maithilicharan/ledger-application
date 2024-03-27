package com.iot.payment.rest;

import com.iot.payment.LedgerApplication;
import com.iot.payment.exception.ApiError;
import com.iot.payment.querymodel.HistoricalBalanceResponse;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = LedgerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LedgerRestEndpointIntegrationTest {
    public static final String NON_EXISTING_ENTITY = "nonexistentEntity";
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
    @DisplayName("Should successfully update account status")
    public void shouldUpdateAccountStatus() {
        WebClient client = WebClient.builder()
                .clientConnector(httpConnector())
                .build();
        String entityId = UUID.randomUUID().toString();
        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/entity/" + entityId)))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
        StepVerifier.create(retrieveResponse(client.put()
                        .uri("http://localhost:" + port + "/change-account-status" + "/entity/" + entityId + "/new-account-status/CLOSED")))
                .verifyComplete();
    }


    @Test
    @DisplayName("Should fail to update account status when entity does not exist")
    public void shouldFailToUpdateAccountStatusWhenEntityDoesNotExist() {
        WebClient client = WebClient.builder()
                .clientConnector(httpConnector())
                .build();

        StepVerifier.create(retrieveResponse(client.put()
                        .uri("http://localhost:" + port + "/change-account-status" + "/entity/" + NON_EXISTING_ENTITY + "/new-account-status/CLOSED")))
                .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException)
                .verify();
    }

    @Test
    @DisplayName("Should successfully transfer balances")
    public void shouldTransferBalances() {
        WebClient client = WebClient.builder()
                .clientConnector(httpConnector())
                .build();
        String entityId = UUID.randomUUID().toString();
        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/entity/" + entityId)))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/transfer/entity/" + entityId + "/source-id/FiatCurrencySourceWalletId/destination-id/FiatCurrencyDestinationWalletId/amount/100")))
                .verifyComplete();
    }

    @Ignore // its polluting the logs
    @DisplayName("Should fail to transfer balances when entity does not exist")
    public void shouldFailToTransferBalancesWhenEntityDoesNotExist() {
        WebClient client = WebClient.builder()
                .clientConnector(httpConnector())
                .build();

        StepVerifier.create(retrieveResponse(client.post()
                        .uri("http://localhost:" + port + "/transfer/entity/" + NON_EXISTING_ENTITY + "/source-id/FiatCurrencySourceWalletId/destination-id/FiatCurrencyDestinationWalletId/amount/100")))
                .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException)
                .verify();
    }

    @Test
    @DisplayName("Should handle ResponseStatusException and return ApiError response")
    public void shouldHandleResponseStatusException() {
        LedgerRestEndpoint ledgerRestEndpoint = new LedgerRestEndpoint(null, null);
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Bad request");
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> responseEntity = ledgerRestEndpoint.handleResponseStatusException(ex, request);
        ApiError apiError = (ApiError) responseEntity.getBody();

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        assertEquals("Bad request", apiError.message());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return ApiError response")
    public void shouldHandleIllegalArgumentException() {
        LedgerRestEndpoint ledgerRestEndpoint = new LedgerRestEndpoint(null, null);
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> responseEntity = ledgerRestEndpoint.handleIllegalArgumentException(ex, request);
        ApiError apiError = (ApiError) responseEntity.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Invalid argument", apiError.message());
    }

    @Test
    @DisplayName("Should handle general Exception and return ApiError response")
    public void shouldHandleGeneralException() {
        LedgerRestEndpoint ledgerRestEndpoint = new LedgerRestEndpoint(null, null);
        Exception ex = new Exception("General exception");
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> responseEntity = ledgerRestEndpoint.handleAll(ex, request);
        ApiError apiError = (ApiError) responseEntity.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("General exception", apiError.message());
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

    private Mono<LedgerRestEndpointIntegrationTest.ResponseList> retrieveListResponse(WebClient.RequestHeadersSpec<?> spec) {
        return spec.accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(LedgerRestEndpointIntegrationTest.ResponseList.class);
    }

    private static class ResponseList extends ArrayList<HistoricalBalanceResponse> {

        private ResponseList() {
            super();
        }
    }
}
