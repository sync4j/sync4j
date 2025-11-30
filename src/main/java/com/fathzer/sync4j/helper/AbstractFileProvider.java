package com.fathzer.sync4j.helper;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;

/**
 * Abstract base class for FileProvider implementations.
 * <p>
 * This class provides a common implementation for managing read-only mode,
 * hash algorithm support, and fast listing capabilities. Concrete subclasses
 * need only implement the file system-specific operations.
 * </p>
 */
public abstract class AbstractFileProvider implements FileProvider {
    private boolean readOnly;
    private final boolean readOnlySupported;
    private final List<HashAlgorithm> supportedHashAlgorithms;
    private final boolean fastListSupported;

    /**
     * Constructs a new AbstractFileProvider with the specified capabilities.
     * <p>
     * The provider is initially created in read-write mode (not read-only).
     * </p>
     * 
     * @param readOnlySupported whether this provider can operate in read-only mode
     * @param supportedHashAlgorithms list of hash algorithms that this provider supports
     * @param fastListSupported whether this provider supports fast listing operations
     */
    public AbstractFileProvider(boolean readOnlySupported, List<HashAlgorithm> supportedHashAlgorithms, boolean fastListSupported) {
        this.readOnly = false;
        this.readOnlySupported = readOnlySupported;
        this.supportedHashAlgorithms = supportedHashAlgorithms;
        this.fastListSupported = fastListSupported;
    }

    @Override
    public boolean isReadOnlySupported() {
        return readOnlySupported;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return supportedHashAlgorithms;
    }

    @Override
    public boolean isFastListSupported() {
        return fastListSupported;
    }

    /**
     * Checks if the provider is read-only and throws an exception if it is.
     * @throws IOException if the provider is read-only
     */
    protected void checkReadOnly() throws IOException {
        if (isReadOnly()) {
            throw new IOException("Provider is read-only");
        }
    }
}
