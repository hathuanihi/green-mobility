package com.greenmobility.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentBusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Object data;

    public PaymentBusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.data = null;
    }

    public PaymentBusinessException(HttpStatus status, String errorCode, String message, Object data) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.data = data;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}

