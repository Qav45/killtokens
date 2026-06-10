package com.example.killtokens.refined;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YamlRefinedOreStorageTest {

    private File tempDir;
    private YamlRefinedOreStorage storage;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("refined-storage-test").toFile();
        storage = new YamlRefinedOreStorage(tempDir, Logger.getLogger("test"));
    }

    @After
    public void tearDown() throws Exception {
        Files.walk(tempDir.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void defaultsAreEmptyAndAutoStorageOff() {
        UUID uuid = UUID.randomUUID();
        assertEquals(0, storage.getRefinedBalance(uuid));
        assertEquals(0, storage.getCompressedBalance(uuid));
        assertEquals(0, storage.getRefinedTotal(uuid));
        assertEquals(0, storage.getCompressedTotal(uuid));
        assertEquals(0, storage.getRefinedPity(uuid));
        assertEquals(0, storage.getCompressedPity(uuid));
        assertFalse(storage.isAutoStoring(uuid));
    }

    @Test
    public void valuesPersistAcrossReload() {
        UUID uuid = UUID.randomUUID();
        storage.addRefined(uuid, 12);
        storage.addCompressed(uuid, 2);
        storage.addRefinedTotal(uuid, 15);
        storage.addCompressedTotal(uuid, 3);
        storage.setRefinedPity(uuid, 99);
        storage.setCompressedPity(uuid, 4999);
        storage.setAutoStoring(uuid, true);
        storage.flush();

        YamlRefinedOreStorage reloaded = new YamlRefinedOreStorage(tempDir, Logger.getLogger("test"));
        assertEquals(12, reloaded.getRefinedBalance(uuid));
        assertEquals(2, reloaded.getCompressedBalance(uuid));
        assertEquals(15, reloaded.getRefinedTotal(uuid));
        assertEquals(3, reloaded.getCompressedTotal(uuid));
        assertEquals(99, reloaded.getRefinedPity(uuid));
        assertEquals(4999, reloaded.getCompressedPity(uuid));
        assertTrue(reloaded.isAutoStoring(uuid));
    }

    @Test
    public void negativeValuesClampToZero() {
        UUID uuid = UUID.randomUUID();
        storage.setRefinedBalance(uuid, -1);
        storage.setCompressedBalance(uuid, -1);
        storage.setRefinedPity(uuid, -1);
        storage.setCompressedPity(uuid, -1);

        assertEquals(0, storage.getRefinedBalance(uuid));
        assertEquals(0, storage.getCompressedBalance(uuid));
        assertEquals(0, storage.getRefinedPity(uuid));
        assertEquals(0, storage.getCompressedPity(uuid));
    }
}
