package com.javanauta.ts.gateway.exception;

import com.javanauta.ts.apicontract.response.ErrorResponse;
import com.javanauta.ts.gateway.exception.enums.ExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("Authentication exception: {}", authException.getMessage());

        int httpStatusCode = HttpStatus.UNAUTHORIZED.value();
        ExceptionCode exceptionCode = ExceptionCode.AUTHENTICATION_ERROR;

        ErrorResponse errorResponse = new ErrorResponse(
                httpStatusCode,
                exceptionCode.getIdentifier(),
                authException.getMessage(),
                List.of()
        );

        response.setStatus(httpStatusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
