package com.fathzer.sync4j.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A utility class for matching paths against <b>glob</b> style patterns.
 * 
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">FileSystem.getPathMatcher</a>
 * 
 */
public final class GlobPatternMatcher implements Predicate<Path> {
    private final PathMatcher matcher;

    /**
     * Creates a new PatternMatcher with the specified pattern.
     *
     * @param pattern the pattern to match against
     * @throws NullPointerException if pattern is null
     * @throws IllegalArgumentException if the pattern is invalid
     */
    public GlobPatternMatcher(String pattern) {
        matcher = FileSystems.getDefault().getPathMatcher("glob:" + Objects.requireNonNull(pattern));
    }

    @Override
    public boolean test(Path path) {
        return matcher.matches(path);
    }
}
