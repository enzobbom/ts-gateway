package com.javanauta.ts.gateway.exception;

import com.javanauta.ts.apicontract.response.ErrorResponse;
import com.javanauta.ts.apicontract.response.enums.ResponseStatus;
import com.javanauta.ts.gateway.exception.enums.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayErrorControllerTest {
    @Mock
    private ErrorAttributes errorAttributes;

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private GatewayErrorController underTest;

    @ParameterizedTest
    @MethodSource("downstreamUnavailableCauses")
    void handleError_shouldReturnBadGatewayWhenDownstreamServiceIsUnavailable(Throwable cause) {
        Throwable error = new RuntimeException("Gateway request failed", cause);

        when(errorAttributes.getError(webRequest)).thenReturn(error);

        ResponseEntity<ErrorResponse> result = underTest.handleError(webRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(ResponseStatus.ERROR);
        assertThat(result.getBody().getCode()).isEqualTo(502);
        assertThat(result.getBody().getErrorCode())
                .isEqualTo(
                        ExceptionCode.DOWNSTREAM_SERVICE_UNAVAILABLE
                                .getIdentifier()
                );
        assertThat(result.getBody().getMessage())
                .isEqualTo(
                        ExceptionCode.DOWNSTREAM_SERVICE_UNAVAILABLE
                                .getDefaultMessage()
                );
        assertThat(result.getBody().getValidationErrors()).isEmpty();
    }

    @Test
    void handleError_shouldReturnGatewayTimeoutWhenDownstreamServiceTimesOut() {
        Throwable error =
                new RuntimeException(
                        "Gateway request failed",
                        new SocketTimeoutException("Request timed out")
                );

        when(errorAttributes.getError(webRequest)).thenReturn(error);

        ResponseEntity<ErrorResponse> result = underTest.handleError(webRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(ResponseStatus.ERROR);
        assertThat(result.getBody().getCode()).isEqualTo(504);
        assertThat(result.getBody().getErrorCode())
                .isEqualTo(
                        ExceptionCode.DOWNSTREAM_SERIVCE_TIMEOUT
                                .getIdentifier()
                );
        assertThat(result.getBody().getMessage())
                .isEqualTo(
                        ExceptionCode.DOWNSTREAM_SERIVCE_TIMEOUT
                                .getDefaultMessage()
                );
        assertThat(result.getBody().getValidationErrors()).isEmpty();
    }

    @Test
    void handleError_shouldReturnInternalServerErrorForUnexpectedError() {
        Throwable error = new IllegalStateException("Unexpected error");

        when(errorAttributes.getError(webRequest)).thenReturn(error);

        ResponseEntity<ErrorResponse> result = underTest.handleError(webRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(ResponseStatus.ERROR);
        assertThat(result.getBody().getCode()).isEqualTo(500);
        assertThat(result.getBody().getErrorCode())
                .isEqualTo(
                        ExceptionCode.INTERNAL_SERVER_ERROR
                                .getIdentifier()
                );
        assertThat(result.getBody().getMessage())
                .isEqualTo(
                        ExceptionCode.INTERNAL_SERVER_ERROR
                                .getDefaultMessage()
                );
        assertThat(result.getBody().getValidationErrors()).isEmpty();
    }

    private static Stream<Throwable> downstreamUnavailableCauses() {
        return Stream.of(
                new ConnectException("Connection refused"),
                new ClosedChannelException()
        );
    }
}
