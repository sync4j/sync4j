package com.fathzer.sync4j.sync.parameters;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;

import jakarta.annotation.Nonnull;

/**
 * A comparator used to compare two files.
 */
@FunctionalInterface
public interface FileComparator {
    /**
     * Compares two files.
     * @param f1 the first file
     * @param f2 the second file
     * @return true if the files are the same according to this comparator
     * @throws IOException if an I/O error occurs
     */
    boolean areSame(@Nonnull File f1, @Nonnull File f2) throws IOException;
    
    /**
     * A comparator that compares files by size.
     */
    public static final FileComparator SIZE = (f1, f2) -> f1.getSize() == f2.getSize();

    /**
     * A comparator that compares files by last modified date.
     */
    public static final FileComparator MOD_DATE = (f1, f2) -> f1.getLastModified() == f2.getLastModified();

    /**
     * Returns a comparator that compares files by hash.
     * @param hashAlgorithm the hash algorithm to use
     * @return a comparator that compares files by hash
     */
    public static FileComparator hash(HashAlgorithm hashAlgorithm) {
        return (f1, f2) -> f1.getHash(hashAlgorithm).equals(f2.getHash(hashAlgorithm));
    }

    /**
     * Returns a comparator that compares by a list of comparators.
     * @param comparators the list of comparators
     * @return a comparator that compares that returns true if all comparators return true
     */
    @Nonnull
    public static FileComparator of(@Nonnull Iterable<FileComparator> comparators) {
        return (f1, f2) -> {
            for (FileComparator comparator : comparators) {
                if (!comparator.areSame(f1, f2)) {
                    return false;
                }
            }
            return true;
        };
    }
}
