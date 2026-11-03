package com.pap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Users are authenticated by JWT only, so Boot's generated in-memory user is not wanted
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class PapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PapApiApplication.class, args);
    }
}
