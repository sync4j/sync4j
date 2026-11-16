package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fathzer.sync4j.file.LocalProvider;
import com.fathzer.sync4j.file.LocalProviderFactory;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.memory.MemoryProviderFactory;

class FileProviderFactoryTest {
    @Test
    void testLocalFactory(@TempDir Path tmp) throws IOException {
        final LocalProviderFactory factory = new LocalProviderFactory();

        assertEquals("local", factory.type());
        assertEquals(Void.class, factory.configType());
        assertTrue(factory.isRootPathRequired());
        try (FileProvider provider = factory.create(null).apply(tmp.toString())) {
            assertInstanceOf(LocalProvider.class, provider);
            Files.writeString(tmp.resolve("file.txt"), "content");
            assertTrue(provider.get("/file.txt").isFile());
        }
        // The root has to exist
        assertThrows(RuntimeException.class, () -> factory.create(null).apply(tmp.resolve("missing").toString()));
    }

    @Test
    void testMemoryFactory() throws IOException {
        final MemoryProviderFactory factory = new MemoryProviderFactory();

        assertEquals("memory", factory.type());
        assertEquals(Void.class, factory.configType());
        assertFalse(factory.isRootPathRequired(), "A memory provider has no root path");
        try (FileProvider first = factory.create(null).apply(""); FileProvider second = factory.create(null).apply("")) {
            assertInstanceOf(MemoryFileProvider.class, first);
            // Each provider is a new empty file system
            assertNotSame(first, second);
            assertTrue(first.get("").isFolder());
        }
        // There is no folder but the root
        assertThrows(IOException.class, () -> factory.create(null).apply("/folder"));
    }

    @Test
    void testFactoriesAreDeclaredAsServices() {
        // This is how a program that reads configuration files finds the providers of this library
        @SuppressWarnings("rawtypes")
        final List<String> types = new ArrayList<>();
        for (FileProviderFactory<?> factory : ServiceLoader.load(FileProviderFactory.class)) {
            types.add(factory.type());
        }

        assertEquals(List.of("local", "memory"), types.stream().sorted().toList());
    }
}
