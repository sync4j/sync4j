package com.fathzer.sync4j.sync;

import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    public record Counter(AtomicLong total, AtomicLong done) {
        Counter() {
            this(new AtomicLong(), new AtomicLong());
        }
    }

    public Counter preloadedFolders = new Counter();
    private Counter listedFolders = new Counter();
    private Counter checkedFiles = new Counter();
    private Counter deletedFiles = new Counter();
    private Counter copiedFiles = new Counter();
    private Counter copiedBytes = new Counter();
    private Counter deletedFolders = new Counter();
    private Counter createdFolders = new Counter();
    private AtomicLong skippedFiles = new AtomicLong();
    private AtomicLong skippedFolders = new AtomicLong();

    public Counter preloadedFolders() { return preloadedFolders; }
    public Counter listedFolders() { return listedFolders; }
    public Counter checkedFiles() { return checkedFiles; }
    public Counter deletedFiles() { return deletedFiles; }
    public Counter deletedFolders() { return deletedFolders; }
    public Counter copiedFiles() { return copiedFiles; }
    public Counter copiedBytes() { return copiedBytes; }
    public Counter createdFolders() { return createdFolders; }
    public AtomicLong skippedFiles() { return skippedFiles; }
    public AtomicLong skippedFolders() { return skippedFolders; }
    @Override
    public String toString() {
        return "Statistics [preloadedFolders=" + preloadedFolders + ", listedFolders=" + listedFolders + ", checkedFiles=" + checkedFiles + ", deletedFiles=" + deletedFiles + ", copiedFiles="
                + copiedFiles + ", copiedBytes=" + copiedBytes + ", deletedFolders=" + deletedFolders
                + ", createdFolders=" + createdFolders + ", skippedFiles=" + skippedFiles
                + ", skippedFolders=" + skippedFolders + "]";
    }

    
}
