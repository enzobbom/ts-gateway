package com.javanauta.ts.gateway.routing.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String query = request.getQueryString();
        String uri = query != null
                ? request.getRequestURI() + "?" + query
                : request.getRequestURI();

        log.info(
                "Incoming request '{} {}' from={}",
                request.getMethod(),
                uri,
                request.getRemoteAddr()
        );

        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);

        int status = response.getStatus();
        if (status >= 200 && status < 400) {
            long duration = System.currentTimeMillis() - start;

            log.info(
                    "Request '{} {}' from={} returned status={} with duration={}ms",
                    request.getMethod(),
                    uri,
                    request.getRemoteAddr(),
                    status,
                    duration
            );
        }
    }
}