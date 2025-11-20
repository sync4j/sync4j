package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashAlgorithmTest {
    private static final String TEST_STRING = "Hello, World!";
    private static final byte[] TEST_BYTES = TEST_STRING.getBytes(StandardCharsets.UTF_8);
    
    // Expected hashes for "Hello, World!"
    private static final String EXPECTED_MD5 = "65a8e27d8879283831b664bd8b7f0ad4";
    private static final String EXPECTED_SHA1 = "0a0a9f2a6772942557ab5355d76af442f8f65e01";
    private static final String EXPECTED_SHA256 = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

    @Test
    void testGetAlgorithmName() {
        assertEquals("SHA-1", HashAlgorithm.SHA1.getAlgorithmName());
        assertEquals("SHA-256", HashAlgorithm.SHA256.getAlgorithmName());
        assertEquals("MD5", HashAlgorithm.MD5.getAlgorithmName());
    }

    @Test
    void testComputeHashByteArray() {
        assertEquals(EXPECTED_MD5, HashAlgorithm.MD5.computeHash(TEST_BYTES));
        assertEquals(EXPECTED_SHA1, HashAlgorithm.SHA1.computeHash(TEST_BYTES));
        assertEquals(EXPECTED_SHA256, HashAlgorithm.SHA256.computeHash(TEST_BYTES));
    }

    @Test
    void testComputeHashInputStream() throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(TEST_BYTES)) {
            assertEquals(EXPECTED_MD5, HashAlgorithm.MD5.computeHash(input));
        }
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(TEST_BYTES)) {
            assertEquals(EXPECTED_SHA1, HashAlgorithm.SHA1.computeHash(input));
        }
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(TEST_BYTES)) {
            assertEquals(EXPECTED_SHA256, HashAlgorithm.SHA256.computeHash(input));
        }
    }

    @Test
    void testComputeHashFile(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, TEST_BYTES);
        
        // Test each algorithm
        assertEquals(EXPECTED_MD5, HashAlgorithm.MD5.computeHash(testFile));
        assertEquals(EXPECTED_SHA1, HashAlgorithm.SHA1.computeHash(testFile));
        assertEquals(EXPECTED_SHA256, HashAlgorithm.SHA256.computeHash(testFile));
    }

    @Test
    void testComputeHashEmptyInput() {
        byte[] emptyBytes = new byte[0];
        
        // Expected hashes of empty string
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", HashAlgorithm.MD5.computeHash(emptyBytes));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", HashAlgorithm.SHA1.computeHash(emptyBytes));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 
            HashAlgorithm.SHA256.computeHash(emptyBytes));
    }

    @Test
    void testComputeHashNullInput() {
        // Test null byte array
        assertThrows(NullPointerException.class, () -> HashAlgorithm.MD5.computeHash((byte[]) null));
        
        // Test null input stream
        assertThrows(NullPointerException.class, () -> {
            try (java.io.InputStream is = null) {
                HashAlgorithm.MD5.computeHash(is);
            }
        });
        
        // Test null path
        assertThrows(NullPointerException.class, () -> HashAlgorithm.MD5.computeHash((Path) null));
    }
}
