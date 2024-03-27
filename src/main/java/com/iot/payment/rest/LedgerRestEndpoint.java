package com.iot.payment.rest;

import com.iot.payment.commandmodel.AccountState;
import com.iot.payment.commandmodel.PostingState;
import com.iot.payment.coreapi.commands.CreateEntityCommand;
import com.iot.payment.coreapi.commands.ModifyPostingCommand;
import com.iot.payment.coreapi.commands.TransferCommand;
import com.iot.payment.coreapi.commands.UpdateAccountStatusCommand;
import com.iot.payment.exception.ApiError;
import com.iot.payment.querymodel.EntityQueryService;
import com.iot.payment.querymodel.HistoricalBalanceResponse;
import lombok.AllArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@AllArgsConstructor
public class LedgerRestEndpoint {
    public static final String MINIMUM_ACCOUNT = "ACCOUNT1";
    private final CommandGateway commandGateway;
    private final EntityQueryService entityQueryService;

    @PostMapping("/transfer/entity/{entity-id}/source-id/{source-id}/destination-id/{destination-id}/amount/{amount}")
    public CompletableFuture<Void> transfer(@PathVariable("entity-id") String entityId, @PathVariable("source-id") String source, @PathVariable("destination-id") String destination, @PathVariable("amount") int amount) {
        return commandGateway.send(new TransferCommand(entityId, source, destination, new BigDecimal(amount)));
    }

    @PostMapping("/entity")
    public CompletableFuture<String> createEntity() {
        return createEntity(UUID.randomUUID()
                .toString());
    }

    @PostMapping("/entity/{entity-id}")
    public CompletableFuture<String> createEntity(@PathVariable("entity-id") String entityId) {
        return commandGateway.send(new CreateEntityCommand(entityId));
    }

    @PutMapping("/change-account-status/entity/{entity-id}/new-account-status/{new-account-status}")
    public CompletableFuture<Void> changeAccountStatus(@PathVariable("entity-id") String entityId, @PathVariable("new-account-status") String newAccountStatus) {
        return commandGateway.send(new UpdateAccountStatusCommand(entityId, MINIMUM_ACCOUNT, AccountState.valueOf(newAccountStatus)));
    }

    @PutMapping("/modify-posting/entity/{entity-id}/source-id/{source-id}/destination-id/{destination-id}/posting-id/{posting-id}/posting-state/{posting-state}/new-amount/{new-amount}")
    public CompletableFuture<Void> modifyPosting(@PathVariable("entity-id") String entityId, @PathVariable("source-id") String source,
                                                 @PathVariable("destination-id") String destination, @PathVariable("posting-id") String postingId, @PathVariable("posting-state") String postingState, @PathVariable("new-amount") int newAmount) {
        return commandGateway.send(new ModifyPostingCommand(entityId, source, destination, postingId, new BigDecimal(newAmount), PostingState.valueOf(postingState)));
    }


    @GetMapping("/historical-balance")
    public CompletableFuture<List<HistoricalBalanceResponse>> historicalBalance() {
        return entityQueryService.findHistoricalBalanceOfWallet();
    }

    @ExceptionHandler({ResponseStatusException.class})
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        ApiError apiError = new ApiError(
                HttpStatus.resolve(ex.getStatusCode().value()), ex.getReason(), "error occurred");
        return new ResponseEntity<Object>(
                apiError, new HttpHeaders(), apiError.status());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST, ex.getMessage(), "Bad request");
        return new ResponseEntity<Object>(
                apiError, new HttpHeaders(), apiError.status());
    }

    @ExceptionHandler({Exception.class}) // This will handle all other types of exceptions
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage(), "internal server error occurred");
        return new ResponseEntity<Object>(
                apiError, new HttpHeaders(), apiError.status());
    }
}
