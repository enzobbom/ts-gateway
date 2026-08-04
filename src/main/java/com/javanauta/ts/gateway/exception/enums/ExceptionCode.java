package com.javanauta.ts.gateway.exception.enums;

import lombok.Getter;

@Getter
public enum ExceptionCode {
    AUTHENTICATION_ERROR ("Authentication failed"),
    DOWNSTREAM_SERVICE_UNAVAILABLE ("Downstream service unavailable"),
    DOWNSTREAM_SERIVCE_TIMEOUT ("Connection to downstream service timed out"),
    INTERNAL_SERVER_ERROR ("Internal server error");

    private final String defaultMessage;

    ExceptionCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getIdentifier() {
        return this.name();
    }
}
