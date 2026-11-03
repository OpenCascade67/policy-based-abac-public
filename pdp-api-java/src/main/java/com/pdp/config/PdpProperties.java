package com.pdp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Typed application settings bound from the {@code pdp.*} properties.
 *
 * @param maxBatchSize largest number of individual decisions in one batch or XACML multiple-decision request
 */
@Validated
@ConfigurationProperties(prefix = "pdp")
public record PdpProperties(
        @Valid @NotNull PolicyStore policyStore,
        @Min(1) @Max(10_000) int maxBatchSize,
        @Valid @NotEmpty List<Client> clients,
        @Valid @NotNull Cors cors) {

    /**
     * @param folder           the PAP's JSON data folder (policies.json, shared-rules.json, domains.json); read-only
     * @param refreshInterval  how often to check the files for changes; zero checks on every request
     */
    public record PolicyStore(@NotBlank String folder, @NotNull Duration refreshInterval) {
    }

    /**
     * A PEP application allowed to request decisions.
     *
     * @param name     identifies the client in logs
     * @param apiKey   secret sent in the {@code X-API-Key} header
     * @param domains  domains the client may query; empty means all domains
     */
    public record Client(@NotBlank String name, @NotBlank @Size(min = 16) String apiKey, List<UUID> domains) {

        public Client {
            domains = domains == null ? List.of() : List.copyOf(domains);
        }
    }

    /** @param allowedOriginPatterns browser origins allowed to call the API; usually empty for service-to-service use */
    public record Cors(List<String> allowedOriginPatterns) {

        public Cors {
            allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
        }
    }
}
