package com.fathzer.sync4j.helper;

import com.fathzer.sync4j.FileProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utility class for path operations.
 */
public final class PathUtils {
    private PathUtils() {
    }

    /**
     * Returns true if the path is the root path.
     * @param path the path to check
     * @return true if the path is the root path
     */
    public static boolean isRoot(@Nullable String path) {
        return FileProvider.ROOT_PATH.equals(path);
    }
    
    /**
     * Returns the parent path of the given path.
     * @param path the path to get the parent path of
     * @return the parent path of the given path (null if the path is the root path)
     */
    @Nullable
    public static String getParent(@Nonnull String path) {
        return isRoot(path) ? null : path.substring(0, path.lastIndexOf('/'));
    }

    /**
     * Returns the name of the given path.
     * @param path the path to get the name of
     * @return the name of the given path
     */
    @Nonnull
    public static String getName(@Nonnull String path) {
        return isRoot(path) ? FileProvider.ROOT_PATH : path.substring(path.lastIndexOf('/') + 1);
    }
}
