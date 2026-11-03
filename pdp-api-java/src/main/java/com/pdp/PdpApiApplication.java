package com.pdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Clients authenticate with API keys only, so Boot's generated in-memory user is not wanted
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class PdpApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdpApiApplication.class, args);
    }
}
