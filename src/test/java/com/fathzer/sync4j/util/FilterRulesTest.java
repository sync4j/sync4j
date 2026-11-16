package com.fathzer.sync4j.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;

class FilterRulesTest {

    private static FilterRules rules(String... rules) {
        return FilterRules.of(List.of(rules));
    }

    @Test
    void testNoRule() {
        final FilterRules none = FilterRules.of(List.of());
        assertTrue(none.isEmpty());
        assertFalse(none.isExcluded("/a.tmp"));
        assertFalse(rules("- *.tmp").isEmpty());
    }

    @Test
    void testUnanchoredPatternsMatchAtAnyLevel() {
        final FilterRules f = rules("- *.tmp");
        assertTrue(f.isExcluded("/a.tmp"));
        assertTrue(f.isExcluded("/x/y/a.tmp"));
        assertFalse(f.isExcluded("/a.txt"));
        assertFalse(f.isExcluded("/a.tmp/b.txt"), "Only the entry itself is tested");
        // A pattern with a folder
        final FilterRules sub = rules("- photos/*.tmp");
        assertTrue(sub.isExcluded("/photos/a.tmp"));
        assertTrue(sub.isExcluded("/x/photos/a.tmp"));
        assertFalse(sub.isExcluded("/photos/x/a.tmp"));
        assertFalse(sub.isExcluded("/myphotos/a.tmp"));
        // Anything, for the name of a folder
        assertTrue(rules("- cache").isExcluded("/x/cache"));
        assertFalse(rules("- cache").isExcluded("/x/mycache"));
    }

    @Test
    void testAnchoredPatterns() {
        final FilterRules f = rules("- /photos/*.tmp");
        assertTrue(f.isExcluded("/photos/a.tmp"));
        assertFalse(f.isExcluded("/x/photos/a.tmp"));
        assertFalse(f.isExcluded("/photos/x/a.tmp"));
        assertTrue(rules("- /cache").isExcluded("/cache"));
        assertFalse(rules("- /cache").isExcluded("/x/cache"));
    }

    @Test
    void testDoubleStar() {
        assertTrue(rules("- /photos/**.tmp").isExcluded("/photos/a.tmp"));
        assertTrue(rules("- /photos/**.tmp").isExcluded("/photos/x/y/a.tmp"));
        // The / after ** is mandatory
        assertFalse(rules("- /photos/**/*.tmp").isExcluded("/photos/a.tmp"));
        assertTrue(rules("- /photos/**/*.tmp").isExcluded("/photos/x/a.tmp"));
        assertTrue(rules("- **/*.tmp").isExcluded("/a.tmp"));
        assertTrue(rules("- /cache/**").isExcluded("/cache/x/y"));
    }

    @Test
    void testGroupsAndCase() {
        final FilterRules f = rules("- *.{xmp,dop}");
        assertTrue(f.isExcluded("/a.xmp"));
        assertTrue(f.isExcluded("/x/a.dop"));
        assertFalse(f.isExcluded("/a.jpg"));
        assertFalse(f.isExcluded("/a.XMP"), "The matching is case sensitive");
    }

    @Test
    void testIncludeBeforeExclude() {
        final FilterRules f = rules("+ /endroit/particulier/**.trucs", "- *.trucs");
        assertTrue(f.isExcluded("/a.trucs"));
        assertTrue(f.isExcluded("/endroit/a.trucs"));
        assertTrue(f.isExcluded("/ailleurs/endroit/particulier/a.trucs"));
        assertFalse(f.isExcluded("/endroit/particulier/a.trucs"));
        assertFalse(f.isExcluded("/endroit/particulier/sub/a.trucs"));
        assertFalse(f.isExcluded("/a.txt"));
    }

    @Test
    void testFirstMatchingRuleWins() {
        // The order matters: this include comes too late
        assertTrue(rules("- *.tmp", "+ /keep/**").isExcluded("/keep/a.tmp"));
        assertFalse(rules("+ /keep/**", "- *.tmp").isExcluded("/keep/a.tmp"));
        // An include alone excludes nothing, everything that is not matched is included
        assertFalse(rules("+ *.tmp").isExcluded("/a.txt"));
        // The way to include only some entries is to end with an exclusion of everything
        final FilterRules only = rules("+ *.jpg", "- **");
        assertFalse(only.isExcluded("/x/a.jpg"));
        assertTrue(only.isExcluded("/x/a.txt"));
    }

    @Test
    void testSpacesAfterTheSign() {
        assertTrue(rules("-   *.tmp").isExcluded("/a.tmp"));
        assertTrue(rules("- *.tmp ").isExcluded("/a.tmp"));
        assertTrue(rules("-\t*.tmp").isExcluded("/a.tmp"));
    }

    @Test
    void testInvalidRules() {
        for (String invalid : List.of("", "-", "- ", "+ ", "*.tmp", "-*.tmp", "! *.tmp", "x *.tmp", "**/*.tmp")) {
            assertThrows(IllegalArgumentException.class, () -> rules(invalid), "'" + invalid + "' should be refused");
        }
        assertThrows(IllegalArgumentException.class, () -> rules("- *.{tmp"), "Unclosed group");
        assertThrows(NullPointerException.class, () -> FilterRules.of(null));
        final List<String> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> FilterRules.of(withNull));
        assertThrows(NullPointerException.class, () -> rules("- *.tmp").isExcluded(null));
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> rules("- ok", "- [a"));
        assertTrue(e.getMessage().contains("[a"), e.getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> rules("*.tmp")).getMessage().contains("'+ pattern'"));
    }

    @Test
    void testEntryFilter() throws IOException {
        final var f = rules("+ /keep/**", "- *.tmp").toEntryFilter();
        assertFalse(f.test(entry("/a.tmp")));
        assertTrue(f.test(entry("/keep/a.tmp")));
        assertTrue(f.test(entry("/a.txt")));

        final Entry broken = mock(Entry.class);
        when(broken.getPath()).thenThrow(new IOException("boom"));
        assertThrows(UncheckedIOException.class, () -> f.test(broken));
    }

    private static Entry entry(String path) throws IOException {
        final Entry entry = mock(Entry.class);
        when(entry.getPath()).thenReturn(path);
        return entry;
    }
}
