package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.sync.test.AbstractNonWritableFileProviderTest;

@Disabled("AbstractNonWritableFileProviderTest not yet finished")
class ReadOnlyProviderTest extends AbstractNonWritableFileProviderTest {
    private static final FileProvider PROVIDER = new ReadOnlyProvider();

    private static class ReadOnlyProvider extends MemoryFileProvider {
        private ReadOnlyProvider() {
            super();
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
