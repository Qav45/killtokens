package com.example.killtokens.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class YamlTokenStorageTest {

    private File tempDir;
    private YamlTokenStorage storage;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("killtokens-test").toFile();
        storage = new YamlTokenStorage(tempDir);
    }

    @After
    public void tearDown() {
        new File(tempDir, "tokens.yml").delete();
        tempDir.delete();
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

        YamlTokenStorage reloaded = new YamlTokenStorage(tempDir);
        assertEquals(42, reloaded.getTokens(uuid));
    }
}
