package com.pdp.security;

import com.pdp.config.PdpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The configured API keys. Only SHA-256 digests are kept in memory, and lookups compare digests in constant
 * time so response timing does not reveal how much of a key was correct.
 */
@Component
public class ApiKeyRegistry {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRegistry.class);

    /** The development key shipped in application.yml. */
    static final String DEVELOPMENT_KEY = "dev-pdp-key-change-me";

    private record Entry(byte[] keyDigest, ApiClient client) {
    }

    private final List<Entry> entries;

    public ApiKeyRegistry(PdpProperties properties) {
        List<Entry> configured = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (PdpProperties.Client client : properties.clients()) {
            if (!seenKeys.add(client.apiKey())) {
                throw new IllegalStateException("Duplicate API key configured for client '" + client.name() + "'");
            }
            if (DEVELOPMENT_KEY.equals(client.apiKey())) {
                log.warn("Client '{}' uses the development API key; set PDP_API_KEY before production use", client.name());
            }
            configured.add(new Entry(sha256(client.apiKey()), new ApiClient(client.name(), Set.copyOf(client.domains()))));
        }
        this.entries = List.copyOf(configured);
    }

    public Optional<ApiClient> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return Optional.empty();
        byte[] digest = sha256(apiKey);
        ApiClient match = null;
        // Check every entry so the time taken does not depend on which key matched
        for (Entry entry : entries) {
            if (MessageDigest.isEqual(entry.keyDigest(), digest)) match = entry.client();
        }
        return Optional.ofNullable(match);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
