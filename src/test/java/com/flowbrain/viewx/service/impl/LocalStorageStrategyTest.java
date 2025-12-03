package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.config.ResourceStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageStrategyTest {

    private LocalStorageStrategy localStorageStrategy;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("viewx_test_uploads");

        ResourceStorageConfig config = new ResourceStorageConfig();
        config.setUploadDir(tempDir.toString());
        config.setBaseUrl("http://localhost:8080/files");

        localStorageStrategy = new LocalStorageStrategy(config);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the temporary directory
        FileSystemUtils.deleteRecursively(tempDir);
    }

    @Test
    void testStoreFile() throws IOException {
        String filename = "test-avatar.jpg";
        String content = "fake image content";
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/jpeg", content.getBytes());

        String storedFilename = localStorageStrategy.storeFile(file, filename);

        assertEquals(filename, storedFilename);
        assertTrue(Files.exists(tempDir.resolve(filename)));
        assertEquals(content, Files.readString(tempDir.resolve(filename)));
    }

    @Test
    void testGetFileUrl() {
        String filename = "test-avatar.jpg";
        String url = localStorageStrategy.getFileUrl(filename);

        assertEquals("http://localhost:8080/files/" + filename, url);
    }

    @Test
    void testDeleteFile() throws IOException {
        // Prepare a file
        String filename = "to-delete.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, "content");
        assertTrue(Files.exists(filePath));

        String fileUrl = "http://localhost:8080/files/" + filename;

        // Execute delete
        boolean deleted = localStorageStrategy.deleteFile(fileUrl);

        assertTrue(deleted);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testDeleteFile_WithNoSlashUrl() throws IOException {
        // Test the fix for extractFilenameFromUrl
        String filename = "simple-filename.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, "content");

        boolean deleted = localStorageStrategy.deleteFile(filename);

        assertTrue(deleted);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testDeleteFile_NotExists() throws IOException {
        String fileUrl = "http://localhost:8080/files/non-existent.jpg";
        boolean deleted = localStorageStrategy.deleteFile(fileUrl);

        assertFalse(deleted);
    }
}
