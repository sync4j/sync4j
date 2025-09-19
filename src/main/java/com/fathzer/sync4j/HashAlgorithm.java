package com.fathzer.sync4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.annotation.Nonnull;

/**
 * Utility class for computing hashes of data using various algorithms.
 */
public enum HashAlgorithm {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256");

    private final String algorithmName;
    private final MessageDigest digest;

    private HashAlgorithm(@Nonnull String algorithmName) {
        this.algorithmName = algorithmName;
        try {
            this.digest = MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new PanicException(e);
        }
    }

    @Nonnull
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Computes the hash of the given byte array.
     *
     * @param data The data to hash
     * @return The hexadecimal string representation of the hash
     */
    @Nonnull
    public String computeHash(@Nonnull byte[] data) {
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    /**
     * Computes the hash of the data from the given input stream.
     * The stream will be read until the end but won't be closed by this method.
     *
     * @param inputStream The input stream to read from
     * @return The hexadecimal string representation of the hash
     * @throws IOException If an I/O error occurs
     */
    @Nonnull
    public String computeHash(@Nonnull InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        
        return bytesToHex(digest.digest());
    }

    /**
     * Computes the hash of the file at the given path.
     *
     * @param filePath  The path to the file
     * @return The hexadecimal string representation of the hash
     * @throws IOException If an I/O error occurs
     */
    @Nonnull
    public String computeHash(@Nonnull Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return computeHash(inputStream);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert
     * @return The hexadecimal string representation
     */
    @Nonnull
    private String bytesToHex(@Nonnull byte[] bytes) {
        final StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
