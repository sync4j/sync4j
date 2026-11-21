package com.fathzer.sync4j.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.fathzer.sync4j.Entry;

import jakarta.annotation.Nonnull;

/**
 * Decides which entries are synchronized, from an ordered list of include/exclude rules inspired by the filter rules of <i>rclone</i>.
 * <p>
 * A rule is a sign, a space and a glob pattern (see {@link GlobPatternMatcher}, the matching is case sensitive):
 * </p>
 * <ul>
 * <li><code>- pattern</code> excludes the entries that match the pattern,</li>
 * <li><code>+ pattern</code> includes them.</li>
 * </ul>
 * <p>
 * The rules are tried in order and <b>the first one that matches an entry decides</b>. An entry that no rule matches is included.
 * So, an exception has to be written before the general rule: <code>["+ /photos/**.tmp", "- *.tmp"]</code> excludes all the <code>.tmp</code> files, except the ones in the <code>/photos</code> folder.
 * </p>
 * <p>
 * The patterns are matched against the path of the entry, relative to the root of the synchronization, which starts with <code>/</code> (<code>/photos/a.tmp</code>):
 * </p>
 * <ul>
 * <li>A pattern that starts with <code>/</code> is anchored to the root: <code>/photos/*.tmp</code> matches <code>/photos/a.tmp</code> but not <code>/x/photos/a.tmp</code>.</li>
 * <li>Any other pattern can match at any level: <code>*.tmp</code> matches <code>/a.tmp</code> and <code>/x/y/a.tmp</code>, and <code>photos/*.tmp</code> matches <code>/photos/a.tmp</code> and <code>/x/photos/a.tmp</code>.</li>
 * <li><code>*</code> and <code>?</code> do not match <code>/</code>, <code>**</code> does: <code>/photos/**.tmp</code> matches the <code>.tmp</code> files of <code>/photos</code> and of all its sub folders
 * (<code>/photos/**&#47;*.tmp</code> only matches the ones of its sub folders, because the <code>/</code> after <code>**</code> is mandatory).</li>
 * <li>The rules apply to the files and to the folders: <code>- /cache</code> excludes the <code>cache</code> folder of the root and, as an excluded folder is not visited, everything it contains.</li>
 * </ul>
 * <p>
 * Warning: for the same reason, a rule can't include something that is in an excluded folder: to exclude the <code>.tmp</code> files except some,
 * exclude the files (<code>- *.tmp</code>), not their folders.
 * </p>
 */
public final class FilterRules {
    private static final FilterRules NONE = new FilterRules(List.of());

    private record Rule(GlobPatternMatcher matcher, boolean include) {}

    private final List<Rule> rules;

    private FilterRules(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Creates the rules.
     * @param rules The rules, in order (the first one that matches an entry wins). An empty list includes everything.
     * @return the rules
     * @throws NullPointerException if the list or one of the rules is null
     * @throws IllegalArgumentException if a rule does not start with <code>+ </code> or <code>- </code>, has no pattern, or has an invalid glob pattern
     */
    public static FilterRules of(@Nonnull List<String> rules) {
        Objects.requireNonNull(rules);
        if (rules.isEmpty()) {
            return NONE;
        }
        return new FilterRules(rules.stream().map(FilterRules::toRule).toList());
    }

    private static Rule toRule(String rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        if (rule.length() < 2 || (rule.charAt(0) != '+' && rule.charAt(0) != '-') || !Character.isWhitespace(rule.charAt(1))) {
            throw new IllegalArgumentException("Invalid filter rule '" + rule + "': it should be '+ pattern' (include) or '- pattern' (exclude)");
        }
        final String pattern = rule.substring(2).strip();
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Invalid filter rule '" + rule + "': the pattern is empty");
        }
        try {
            // A pattern that is not anchored to the root can match at any level (one that starts with ** already does)
            final String glob = pattern.startsWith("/") || pattern.startsWith("**") ? pattern : "**/" + pattern;
            return new Rule(new GlobPatternMatcher(glob, true), rule.charAt(0) == '+');
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid filter rule '" + rule + "': " + e.getMessage(), e);
        }
    }

    /**
     * Tests whether a path is excluded.
     * @param path The path of an entry (relative to the root of the synchronization, starting with <code>/</code>)
     * @return true if the entry is excluded
     */
    public boolean isExcluded(@Nonnull String path) {
        Objects.requireNonNull(path);
        for (Rule rule : rules) {
            if (rule.matcher().test(path)) {
                return !rule.include();
            }
        }
        return false;
    }

    /**
     * Tests whether there is no rule (everything is included).
     * @return true if there is no rule
     */
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /**
     * Gets these rules as a predicate that accepts the entries that are <b>not</b> excluded (the one expected by the synchronization parameters).
     * @return a predicate (it throws an {@link UncheckedIOException} if the path of an entry can't be read)
     */
    public Predicate<Entry> toEntryFilter() {
        return entry -> {
            try {
                return !isExcluded(entry.getPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
