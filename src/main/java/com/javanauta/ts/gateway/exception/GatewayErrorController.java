package com.javanauta.ts.gateway.exception;

import com.javanauta.ts.apicontract.response.ErrorResponse;
import com.javanauta.ts.gateway.exception.enums.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class GatewayErrorController implements ErrorController {
    private final ErrorAttributes errorAttributes;

    private static final Map<ExceptionCode, HttpStatus> EXCEPTION_CODE_HTTP_STATUS_MAP = Map.of(
            ExceptionCode.DOWNSTREAM_SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY,
            ExceptionCode.DOWNSTREAM_SERIVCE_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
            ExceptionCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(WebRequest webRequest) {
        Throwable error = errorAttributes.getError(webRequest);

        ExceptionCode exceptionCode;
        if (hasCause(error, ConnectException.class) || hasCause(error, ClosedChannelException.class)) {
            exceptionCode = ExceptionCode.DOWNSTREAM_SERVICE_UNAVAILABLE;
            log.warn("Downstream service unavailable: {}", error.getMessage());

        } else if (hasCause(error, SocketTimeoutException.class)) {
            exceptionCode = ExceptionCode.DOWNSTREAM_SERIVCE_TIMEOUT;
            log.warn("Downstream service timeout: {}", error.getMessage());

        } else {
            exceptionCode = ExceptionCode.INTERNAL_SERVER_ERROR;
            log.error("An unexpected error occurred", error);
        }

        HttpStatus httpCode = EXCEPTION_CODE_HTTP_STATUS_MAP.get(exceptionCode);

        ErrorResponse errorResponse = new ErrorResponse(
                httpCode.value(),
                exceptionCode.getIdentifier(),
                exceptionCode.getDefaultMessage(),
                List.of());

        return ResponseEntity.status(httpCode).body(errorResponse);
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        while (error != null) {
            if (type.isInstance(error)) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }
}
