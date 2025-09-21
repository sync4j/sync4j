package com.fathzer.sync4j.parameters;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;

import jakarta.annotation.Nonnull;

@FunctionalInterface
public interface FileComparator {
    boolean areSame(@Nonnull File f1, @Nonnull File f2) throws IOException;
    
    public static final FileComparator SIZE = (f1, f2) -> f1.getSize() == f2.getSize();

    public static final FileComparator MOD_DATE = (f1, f2) -> f1.getLastModified() == f2.getLastModified();

    public static FileComparator hash(HashAlgorithm hashAlgorithm) {
        return (f1, f2) -> f1.getHash(hashAlgorithm).equals(f2.getHash(hashAlgorithm));
    }

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
