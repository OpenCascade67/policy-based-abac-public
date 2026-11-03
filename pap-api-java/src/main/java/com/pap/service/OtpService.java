package com.pap.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Issues short-lived six-digit one-time passwords for email sign-in. Codes are held in memory only. */
@Service
public class OtpService {

    private static final Duration VALIDITY = Duration.ofMinutes(5);

    private record IssuedCode(String code, Instant expiresAt) {
    }

    private final Map<String, IssuedCode> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OtpService(Clock clock) {
        this.clock = clock;
    }

    public String generate(String email) {
        String code = Integer.toString(random.nextInt(100_000, 999_999));
        codes.put(key(email), new IssuedCode(code, clock.instant().plus(VALIDITY)));
        return code;
    }

    public boolean verify(String email, String code) {
        String key = key(email);
        IssuedCode issued = codes.get(key);
        if (issued == null) return false;
        if (clock.instant().isAfter(issued.expiresAt())) {
            codes.remove(key, issued);
            return false;
        }
        return issued.code().equals(code);
    }

    public void clear(String email) {
        codes.remove(key(email));
    }

    private static String key(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
