package com.example.killtokens.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class YamlTokenStorageTest {

    private File tempDir;
    private YamlTokenStorage storage;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("killtokens-test").toFile();
        storage = new YamlTokenStorage(tempDir, Logger.getLogger("test"));
    }

    @After
    public void tearDown() throws Exception {
        Files.walk(tempDir.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void defaultIsZero() {
        assertEquals(0, storage.getTokens(UUID.randomUUID()));
    }

    @Test
    public void setAndGet() {
        UUID uuid = UUID.randomUUID();
        storage.setTokens(uuid, 5);
        assertEquals(5, storage.getTokens(uuid));
    }

    @Test
    public void addTokens() {
        UUID uuid = UUID.randomUUID();
        storage.addTokens(uuid, 3);
        storage.addTokens(uuid, 2);
        assertEquals(5, storage.getTokens(uuid));
    }

    @Test
    public void negativeClampsToZero() {
        UUID uuid = UUID.randomUUID();
        storage.setTokens(uuid, -5);
        assertEquals(0, storage.getTokens(uuid));
    }

    @Test
    public void flushAndReload() throws Exception {
        UUID uuid = UUID.randomUUID();
        storage.setTokens(uuid, 42);
        storage.flush();

        YamlTokenStorage reloaded = new YamlTokenStorage(tempDir, Logger.getLogger("test"));
        assertEquals(42, reloaded.getTokens(uuid));
    }
}
