package com.javanauta.ts.gateway;

import com.javanauta.ts.gateway.properties.GatewayServiceProperties;
import com.javanauta.ts.gateway.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        GatewayServiceProperties.class,
        JwtProperties.class
})
public class GatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
