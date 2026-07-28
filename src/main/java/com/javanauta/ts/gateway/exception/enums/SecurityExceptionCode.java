package com.javanauta.ts.gateway.exception.enums;

import lombok.Getter;

@Getter
public enum SecurityExceptionCode {
    AUTHENTICATION_ERROR ("Authentication failed");

    private final String defaultMessage;

    SecurityExceptionCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getIdentifier() {
        return this.name();
    }
}
