package com.pdp.store;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdp.config.PdpProperties;
import com.pdp.domain.model.Domain;
import com.pdp.domain.model.Policy;
import com.pdp.domain.model.SharedRule;
import com.pdp.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads policies, shared rules and domains from the PAP's JSON data folder (read-only).
 * <p>
 * Files are re-checked at most once per refresh interval and reloaded when their size or modification time
 * changes. A load that fails (e.g. a file caught mid-write) keeps the previous snapshot and is retried on the
 * next check, so decisions always run against a complete, consistent data set.
 */
@Component
public class FilePolicyStore implements PolicyStore {

    private static final Logger log = LoggerFactory.getLogger(FilePolicyStore.class);

    static final String POLICIES_FILE = "policies.json";
    static final String SHARED_RULES_FILE = "shared-rules.json";
    static final String DOMAINS_FILE = "domains.json";

    /** Size and modification time of each data file, used to detect changes cheaply. */
    private record Fingerprint(List<FileStamp> files) {
    }

    private record FileStamp(long size, long modifiedMillis) {
        static final FileStamp MISSING = new FileStamp(-1, -1);
    }

    private final ObjectMapper mapper = JsonSupport.storageMapper();
    private final Path folder;
    private final Duration refreshInterval;
    private final Clock clock;

    private volatile PolicySnapshot snapshot;
    private volatile Fingerprint loadedFingerprint;
    private volatile Instant lastCheck;
    private volatile String lastError;

    public FilePolicyStore(PdpProperties properties, Clock clock) {
        this.folder = Path.of(properties.policyStore().folder()).toAbsolutePath().normalize();
        this.refreshInterval = properties.policyStore().refreshInterval();
        this.clock = clock;
        this.snapshot = PolicySnapshot.empty(clock.instant());
        if (!Files.isDirectory(folder)) log.warn("Policy folder {} does not exist yet; serving no policies", folder);
        refresh(clock.instant());
    }

    @Override
    public PolicySnapshot current() {
        Instant now = clock.instant();
        if (isCheckDue(now)) refresh(now);
        return snapshot;
    }

    @Override
    public StoreStatus status() {
        return new StoreStatus(Files.isDirectory(folder) && lastError == null, snapshot, lastError);
    }

    private boolean isCheckDue(Instant now) {
        Instant last = lastCheck;
        return last == null || !now.isBefore(last.plus(refreshInterval));
    }

    private synchronized void refresh(Instant now) {
        if (lastCheck != null && !isCheckDue(now)) return; // another thread just checked
        lastCheck = now;

        Fingerprint fingerprint = fingerprint();
        if (fingerprint.equals(loadedFingerprint)) return;

        try {
            PolicySnapshot loaded = new PolicySnapshot(
                    readList(DOMAINS_FILE, Domain.class),
                    readList(POLICIES_FILE, Policy.class),
                    readList(SHARED_RULES_FILE, SharedRule.class),
                    now);
            snapshot = loaded;
            loadedFingerprint = fingerprint;
            lastError = null;
            log.info("Loaded {} policies, {} shared rules and {} domains from {}",
                    loaded.policyCount(), loaded.sharedRuleCount(), loaded.domainCount(), folder);
        } catch (IOException | RuntimeException e) {
            lastError = e.getMessage();
            log.warn("Could not load policy data from {}; keeping the previous snapshot: {}", folder, e.getMessage());
        }
    }

    private <T> List<T> readList(String fileName, Class<T> type) throws IOException {
        Path file = folder.resolve(fileName);
        if (!Files.exists(file)) return List.of();
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        List<T> items = mapper.readValue(file.toFile(), listType);
        return items == null ? List.of() : items;
    }

    private Fingerprint fingerprint() {
        return new Fingerprint(Stream.of(DOMAINS_FILE, POLICIES_FILE, SHARED_RULES_FILE)
                .map(name -> stamp(folder.resolve(name)))
                .toList());
    }

    private static FileStamp stamp(Path file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            return new FileStamp(attributes.size(), attributes.lastModifiedTime().toMillis());
        } catch (IOException e) {
            return FileStamp.MISSING;
        }
    }
}
