package com.fathzer.sync4j.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GlobPatternMatcherTest {

    @Test
    void testNullPattern() {
        assertThrows(NullPointerException.class, () -> wildcardMatch("test.txt", null));
    }

    @Test
    void testNullInput() {
        assertThrows(NullPointerException.class, () -> wildcardMatch(null, "*.txt"));
    }

    private static boolean wildcardMatch(String input, String pattern) {
        GlobPatternMatcher matcher = new GlobPatternMatcher(pattern);
        return matcher.test(Paths.get(input));
    }    

    @ParameterizedTest
    @CsvSource({
        "test.txt, test.txt, true",
        "test.txt, test.tx, false",
        "test.txt, test.txt.bak, false",
        "test.txt, a/test.txt, false",
        "*, test.txt, true",
        "*, '', true",
        "?est.txt, test.txt, true",
        "?est.txt, best.txt, true",
        "?est.txt, est.txt, false",
        "te?t.txt, test.txt, true",
        "*.txt, .txt, true",
        "*.txt, a.txt, true",
        "*.txt, a.b.txt, true",
        "*.txt, a.txt.bak, false",
        "'*.{txt,pdf}', a.txt, true",
        "'*.{txt,pdf}', b.pdf, true",
        "file-*.txt, file-123.txt, true",
        "file-*.txt, file-abc.txt, true",
        "file-*.txt, file-123.log, false",
        "file-*.txt, my-file-123.txt, false",
        "file-?.*, file-1.txt, true",
        "file-?.*, file-a.txt, true",
        "file-?.*, file-ab.txt, false",
        "file-?.*, file-.txt, false",
        "[abc].txt, a.txt, true",
        "[abc].txt, b.txt, true",
        "[abc].txt, d.txt, false",
        "[a-c].txt, b.txt, true",
        "[a-c].txt, d.txt, false",
        "[!a-c].txt, d.txt, true",
        "[!a-c].txt, a.txt, false",
        "file[0-9].txt, file1.txt, true",
        "file[0-9].txt, filea.txt, false",
        "file[!0-9].txt, filea.txt, true",
        "file[!0-9].txt, file1.txt, false",
        "file[0-9a-z].txt, filea.txt, true",
        "file[0-9a-z].txt, file1.txt, true",
        "file[0-9a-z].txt, fileA.txt, false",
        "file[-a].txt, file-.txt, true",
        "file[-a].txt, filea.txt, true",
        "/a/*/c.txt, /a/b/c.txt, true",
        "/a/*/c.txt, /a/b/d.txt, false",
        "/a/*/c.txt, /a/c.txt, false",
        "/a/*/c.txt, /a//b1/b2/d.txt, false",
        "/a/**/b.txt, /a/c/b.txt, true",
        "/a/**/b.txt, /a/b//e/b.txt, true",
        "/a/**/b.txt, /a/b.txt, false",
        "/a/**/b.txt, /a/b.txt, false"
    })
    void testPatternMatching(String pattern, String input, boolean expected) {
        assertEquals(expected, wildcardMatch(input, pattern), 
            String.format("Pattern '%s' should %s match '%s'", 
                pattern, expected ? "" : "not", input));
    }

    @Test
    void myTest() {
        assertTrue(wildcardMatch("file.txt", "*.{txt,pdf}"));
        assertTrue(wildcardMatch("file.pdf", "*.{txt,pdf}"));
        assertFalse(wildcardMatch("file.doc", "*.{txt,pdf}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file[.txt", "file[", "file[^", "file[a-", "file[\"})"})
    void testInvalidPatterns(String pattern) {
        // For invalid patterns, an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> wildcardMatch("file.txt", pattern));
    }

    @Test
    void testEmptyPattern() {
        assertTrue(wildcardMatch("", ""));
        assertFalse(wildcardMatch("a", ""));
    }

    @Test
    void testAsteriskPattern() {
        assertTrue(wildcardMatch("", "*"));
        assertTrue(wildcardMatch("a", "*"));
        assertTrue(wildcardMatch("abc", "*"));
        assertTrue(wildcardMatch("any string", "*"));
    }
}
