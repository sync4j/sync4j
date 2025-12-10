package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.Disabled;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.sync.test.AbstractNonWritableFileProviderTest;

@Disabled("AbstractNonWritableFileProviderTest not yet finished")
class ReadOnlyProviderTest extends AbstractNonWritableFileProviderTest {
    private static final FileProvider PROVIDER;

    static {
        try {
            PROVIDER = new ReadOnlyProvider();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class ReadOnlyProvider extends MemoryFileProvider {
        private ReadOnlyProvider() throws IOException {
            super();
            Folder folder = this.get(ROOT_PATH).asFolder().mkdir("folder");
            folder.copy("file.txt", createMockFile("content"), null);
            setReadOnly(true);
        }

        @Override
        public boolean isWriteSupported() {
            return false;
        }
    }

    @Override
    protected FileProvider createFileProvider() {
        return PROVIDER;
    }



}
