package com.fathzer.sync4j.util;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.annotation.Nonnull;

/**
 * A utility class for matching strings against <b>glob</b> style patterns.
 * <p>
 * This implementation is based on the JDK's internal {@code sun.nio.fs.Globs} class,
 * adapted to provide explicit control over case sensitivity.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Glob_(programming)">Glob pattern syntax</a>
 */
public final class GlobPatternMatcher implements Predicate<String> {
    private final Pattern pattern;
    
    private static final String REGEX_META_CHARS = ".^$+{[]|()";
    private static final String GLOB_META_CHARS = "\\*?[{";
    private static final char EOL = 0;

    /**
     * Creates a new PatternMatcher with the specified pattern and case sensitivity.
     *
     * @param pattern the linux style (with '/' as path separator) glob pattern to match against
     * @param caseSensitive whether matching should be case-sensitive
     * @throws NullPointerException if pattern is null
     * @throws IllegalArgumentException if the pattern is invalid
     */
    public GlobPatternMatcher(@Nonnull String pattern, boolean caseSensitive) {
        Objects.requireNonNull(pattern, "pattern cannot be null");
        String regex = toRegexPattern(pattern);
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        this.pattern = Pattern.compile(regex, flags);
    }

    /**
     * Tests if the specified string matches this pattern.
     *
     * @param path the unix style (with '/' as path separator) path string to match
     * @return true if the string matches this pattern, false otherwise
     */
    @Override
    public boolean test(@Nonnull String path) {
        return pattern.matcher(path).matches();
    }

    private static boolean isRegexMeta(char c) {
        return REGEX_META_CHARS.indexOf(c) != -1;
    }

    private static boolean isGlobMeta(char c) {
        return GLOB_META_CHARS.indexOf(c) != -1;
    }

    private static char next(String glob, int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }

    /**
     * Converts a unix like glob pattern to a regular expression.
     * Based on JDK's sun.nio.fs.Globs implementation.
     *
     * @param globPattern the glob pattern
     * @return the equivalent regular expression
     * @throws PatternSyntaxException if the pattern is invalid
     */
    @SuppressWarnings({"java:S6541","java:S3776","java:S135"})
    private static String toRegexPattern(String globPattern) {
        // This method is a port of sun.nio.fs.Globs, with an adaptation to remove windows specific features
        // This port is the reason for the SuppressWarnings annotations (I do not want to change too much the original code)
        boolean inGroup = false;
        StringBuilder regex = new StringBuilder("^");

        int i = 0;
        while (i < globPattern.length()) {
            char c = globPattern.charAt(i++);
            switch (c) {
                case '\\':
                    // Escape special characters
                    if (i == globPattern.length()) {
                        throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
                    }
                    char nextChar = globPattern.charAt(i++);
                    if (isGlobMeta(nextChar) || isRegexMeta(nextChar)) {
                        regex.append('\\');
                    }
                    regex.append(nextChar);
                    break;
                    
                case '/':
                    regex.append(c);
                    break;
                    
                case '[':
                    // Don't match name separator in class
                    regex.append("[[^/]&&[");
                    if (next(globPattern, i) == '^') {
                        // Escape the regex negation char if it appears
                        regex.append("\\^");
                        i++;
                    } else {
                        // Negation
                        if (next(globPattern, i) == '!') {
                            regex.append('^');
                            i++;
                        }
                        // Hyphen allowed at start
                        if (next(globPattern, i) == '-') {
                            regex.append('-');
                            i++;
                        }
                    }
                    boolean hasRangeStart = false;
                    char last = 0;
                    while (i < globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if (c == ']') {
                            break;
                        }
                        if (c == '/') {
                            throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern, i - 1);
                        }
                        // Escape '\', '[' or "&&" for regex class
                        if (c == '\\' || c == '[' || (c == '&' && next(globPattern, i) == '&')) {
                            regex.append('\\');
                        }
                        regex.append(c);

                        if (c == '-') {
                            if (!hasRangeStart) {
                                throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
                            }
                            if ((c = next(globPattern, i++)) == EOL || c == ']') {
                                break;
                            }
                            if (c < last) {
                                throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
                            }
                            regex.append(c);
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    if (c != ']') {
                        throw new PatternSyntaxException("Missing ']'", globPattern, i - 1);
                    }
                    regex.append("]]");
                    break;
                    
                case '{':
                    if (inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                    
                case '}':
                    if (inGroup) {
                        regex.append("))");
                        inGroup = false;
                    } else {
                        regex.append('}');
                    }
                    break;
                    
                case ',':
                    if (inGroup) {
                        regex.append(")|(?:");
                    } else {
                        regex.append(',');
                    }
                    break;
                    
                case '*':
                    if (next(globPattern, i) == '*') {
                        // Crosses directory boundaries
                        regex.append(".*");
                        i++;
                    } else {
                        // Within directory boundary
                        regex.append("[^/]*");
                    }
                    break;
                    
                case '?':
                    regex.append("[^/]");
                    break;

                default:
                    if (isRegexMeta(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
            }
        }

        if (inGroup) {
            throw new PatternSyntaxException("Missing '}'", globPattern, i - 1);
        }

        return regex.append('$').toString();
    }
}
