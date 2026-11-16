package com.fathzer.sync4j;

import java.io.IOException;

import com.fathzer.sync4j.util.IOLambda.IOFunction;

import jakarta.annotation.Nonnull;

/**
 * A factory that creates file providers from a configuration, typically read from a configuration file.
 * <br>It lets an application work with the providers by their type name ("local", "pCloud", ...) without depending on their classes.
 * <p>
 * Each provider module ships its factory, declared, as for {@link java.util.ServiceLoader}, in the
 * <code>META-INF/services/com.fathzer.sync4j.FileProviderFactory</code> file of its jar (the factory needs a public constructor without argument).
 * A library that reads configuration files, like sync4j-config, discovers and uses it.
 * </p>
 * <p>
 * The configuration of the provider is an object of the type <code>T</code>: a class whose properties are the settings of the provider
 * (a <code>record</code>, for instance, <code>record PCloudConfig(Zone zone, String token)</code>).
 * The program that reads the configuration file builds it from the <code>config</code> attribute of the location (sync4j-config uses Jackson, so the class needs no annotation if it is a record),
 * but nothing prevents to create it by hand: this interface does not depend on any file format.
 * <br>The root path of the provider is not part of its configuration: a same configuration (the account of a remote storage, for instance) can
 * be used with different roots, and the root can change from one synchronization to another (to synchronize a sub folder).
 * </p>
 * @param <T> The type of the configuration of the provider, <code>Void</code> if it has no configuration.
 */
public interface FileProviderFactory<T> {
    /**
     * Gets the type of providers created by this factory.
     * @return the name that identifies the provider in a configuration (for instance "pCloud"), it is case sensitive.
     */
    @Nonnull
    String type();

    /**
     * Gets the class of the configuration.
     * @return the class of the configuration. <code>Void.class</code> if the provider has no configuration: {@link #create(Object)} receives null.
     */
    @Nonnull
    Class<T> configType();

    /**
     * Creates the function that opens a provider.
     * @param config the configuration of the provider. It is built by the caller from the configuration file, whose environment variables are already replaced.
     * <br>Its properties are not checked (a property can be null): the factory has to validate it. It is null if the provider has no configuration (see {@link #configType()}).
     * @return a function that takes the root path of the provider and returns a new provider, the caller has to close it.
     * @throws IOException if the configuration is invalid (a missing or invalid value, for instance)
     */
    @Nonnull
    IOFunction<String, FileProvider> create(T config) throws IOException;

    /**
     * Tests if the providers need a root path.
     * @return true (the default) if the providers can't be opened without a root path. A provider that has no notion of root path (like the one in memory) returns false:
     * the path is then optional in the configuration and is the empty string if it is missing.
     */
    default boolean isRootPathRequired() {
        return true;
    }
}
