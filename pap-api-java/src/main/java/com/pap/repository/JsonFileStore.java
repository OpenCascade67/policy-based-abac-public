package com.pap.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pap.config.PapProperties;
import com.pap.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Reads and writes entity collections as JSON array files in the configured data folder.
 * The file format matches the .NET implementation, so both back ends can share one data folder.
 * Writes go to a temporary file first and are then moved into place, so readers never see a partial file.
 */
@Component
public class JsonFileStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileStore.class);

    private final ObjectMapper mapper = JsonSupport.storageMapper();
    private final ConcurrentMap<String, ReadWriteLock> locks = new ConcurrentHashMap<>();
    private final Path folder;

    public JsonFileStore(PapProperties properties) {
        this.folder = Path.of(properties.dataStore().folder()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data folder " + folder, e);
        }
        log.info("Using JSON data folder {}", folder);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public <T> List<T> read(String fileName, JavaType listType) {
        ReadWriteLock lock = lockFor(fileName);
        lock.readLock().lock();
        try {
            return readUnlocked(fileName, listType);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Reads the collection, applies {@code change} and writes the result back, all under the file's write lock. */
    public <T, R> R modify(String fileName, JavaType listType, Function<List<T>, R> change) {
        ReadWriteLock lock = lockFor(fileName);
        lock.writeLock().lock();
        try {
            List<T> items = readUnlocked(fileName, listType);
            R result = change.apply(items);
            writeUnlocked(fileName, listType, items);
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> List<T> readUnlocked(String fileName, JavaType listType) {
        Path file = folder.resolve(fileName);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            List<T> items = mapper.readValue(file.toFile(), listType);
            return items == null ? new ArrayList<>() : new ArrayList<>(items);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    private void writeUnlocked(String fileName, JavaType listType, List<?> items) {
        Path file = folder.resolve(fileName);
        try {
            Path temp = Files.createTempFile(folder, fileName, ".tmp");
            mapper.writerFor(listType).writeValue(temp.toFile(), items);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    private ReadWriteLock lockFor(String fileName) {
        return locks.computeIfAbsent(fileName, f -> new ReentrantReadWriteLock());
    }
}
