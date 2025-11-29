/**
 * In-memory implementation of the FileProvider interface for testing purposes.
 * <p>
 * This package provides a in-memory file provider implementation that stores
 * files and folders in memory using a Map structure.
 * </p>
 * 
 * <h2>Main Components</h2>
 * <ul>
 * <li>{@link com.fathzer.sync4j.memory.MemoryFileProvider} - Main provider managing the in-memory file system</li>
 * <li>{@link com.fathzer.sync4j.memory.MemoryFile} - In-memory file implementation with content stored as byte arrays</li>
 * <li>{@link com.fathzer.sync4j.memory.MemoryFolder} - In-memory folder implementation managing child entries</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try (MemoryFileProvider provider = new MemoryFileProvider()) {
 *   // Create a folder
 *   provider.createFolder("/documents");
 * 
 *   // Create a file
 *   byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
 *   File file = provider.createFile("/documents/test.txt", content);
 * 
 *   // Read file content
 *   try (InputStream is = file.getInputStream()) {
 *     byte[] data = is.readAllBytes();
 *   }
 * 
 *   // Compute hash
 *   String hash = file.getHash(HashAlgorithm.SHA256);
 * }
 * }</pre>
 * 
 * @since 0.0.1
 */
package com.fathzer.sync4j.memory;
