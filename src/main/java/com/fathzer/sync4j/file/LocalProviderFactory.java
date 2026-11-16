package com.fathzer.sync4j.file;

import java.nio.file.Paths;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.FileProviderFactory;
import com.fathzer.sync4j.util.IOLambda.IOFunction;

import jakarta.annotation.Nonnull;

/**
 * The factory of the local file system provider (type "local"). The provider has no configuration, its root is a folder of the file system.
 */
public class LocalProviderFactory implements FileProviderFactory<Void> {
    /** The type of the provider. */
    public static final String TYPE = "local";

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
    @Nonnull
    public IOFunction<String, FileProvider> create(Void config) {
        return root -> new LocalProvider(Paths.get(root));
    }
}
