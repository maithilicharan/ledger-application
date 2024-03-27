package com.iot.payment.exception;

import org.springframework.http.HttpStatus;

public record ApiError(HttpStatus status, String message, String error) {
}
