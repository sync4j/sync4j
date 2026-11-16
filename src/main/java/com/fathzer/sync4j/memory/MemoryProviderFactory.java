package com.fathzer.sync4j.memory;

import java.io.IOException;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.FileProviderFactory;
import com.fathzer.sync4j.util.IOLambda.IOFunction;

import jakarta.annotation.Nonnull;

/**
 * The factory of the in memory provider (type "memory"). The provider has no configuration, and no root path: it always starts with an empty root folder.
 */
public class MemoryProviderFactory implements FileProviderFactory<Void> {
    /** The type of the provider. */
    public static final String TYPE = "memory";

    @Override
    @Nonnull
    public String type() {
        return TYPE;
    }

    @Override
    @Nonnull
    public Class<Void> configType() {
        return Void.class;
    }

    @Override
    public boolean isRootPathRequired() {
        return false;
    }

    @Override
    @Nonnull
    public IOFunction<String, FileProvider> create(Void config) {
        return root -> {
            // The provider is empty, there is no other folder than its root
            if (!root.isEmpty()) {
                throw new IOException("The memory provider has no folder " + root + ", its root is the only folder it has");
            }
            return new MemoryFileProvider();
        };
    }
}
