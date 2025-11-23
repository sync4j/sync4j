package com.fathzer.sync4j.sync;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Nonnull;

/**
 * Tracks and manages statistics for synchronization operations.
 * This class provides counters and metrics to monitor the progress and status
 * of file synchronization processes.
 */
public class Statistics {
    /**
     * Creates a new Statistics instance with all counters initialized to zero.
     */
    public Statistics() {
        // All counters are initialized with their default values
    }
    
    /**
     * A counter that tracks the number of tasks completed versus the total number of tasks.
     * <p>
     * Note: The total number of tasks may increase during synchronization as more entries are discovered.
     *
     * @param total The total number of tasks to be processed
     * @param done The number of tasks that have been completed
     */
    public record Counter(
        @Nonnull AtomicLong total,
        @Nonnull AtomicLong done) {
        /**
         * Creates a new counter with total and done set to 0.
         */
        Counter() {
            this(Objects.requireNonNull(new AtomicLong()), Objects.requireNonNull(new AtomicLong()));
        }
        public Counter(long total, long done) {
            this(new AtomicLong(total), new AtomicLong(done));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Long.hashCode(total.get());
            result = prime * result + Long.hashCode(done.get());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Counter other = (Counter) obj;
            return total.get() == other.total.get() && done.get() == other.done.get();
        }
    }

    private Counter preloadedFolders = new Counter();
    private Counter listedFolders = new Counter();
    private Counter checkedFiles = new Counter();
    private Counter deletedFiles = new Counter();
    private Counter copiedFiles = new Counter();
    private Counter copiedBytes = new Counter();
    private Counter deletedFolders = new Counter();
    private Counter createdFolders = new Counter();
    private AtomicLong skippedFiles = new AtomicLong();
    private AtomicLong skippedFolders = new AtomicLong();

    /**
     * Returns the counter of preloaded folders.
     * @return the counter of preloaded folders
     */
    public Counter preloadedFolders() { return preloadedFolders; }
    /**
     * Returns the counter of listed folders.
     * @return the counter of listed folders
     */
    public Counter listedFolders() { return listedFolders; }
    /**
     * Returns the counter of checked files.
     * @return the counter of checked files
     */
    public Counter checkedFiles() { return checkedFiles; }
    /**
     * Returns the counter of deleted folders.
     * @return the counter of deleted folders
     */
    public Counter deletedFolders() { return deletedFolders; }
    /**
     * Returns the counter of deleted files.
     * @return the counter of deleted files
     */
    public Counter deletedFiles() { return deletedFiles; }
    /**
     * Returns the counter of copied files.
     * @return the counter of copied files
     */
    public Counter copiedFiles() { return copiedFiles; }
    /**
     * Returns the counter of copied bytes.
     * @return the counter of copied bytes
     */
    public Counter copiedBytes() { return copiedBytes; }
    /**
     * Returns the counter of created folders.
     * @return the counter of created folders
     */
    public Counter createdFolders() { return createdFolders; }
    /**
     * Returns the number of skipped files.
     * @return the number of skipped files
     */
    public AtomicLong skippedFiles() { return skippedFiles; }
    /**
     * Returns the number of skipped folders.
     * @return the number of skipped folders
     */
    public AtomicLong skippedFolders() { return skippedFolders; }
    @Override
    public String toString() {
        return "Statistics [preloadedFolders=" + preloadedFolders + ", listedFolders=" + listedFolders + ", checkedFiles=" + checkedFiles + ", deletedFiles=" + deletedFiles + ", copiedFiles="
                + copiedFiles + ", copiedBytes=" + copiedBytes + ", deletedFolders=" + deletedFolders
                + ", createdFolders=" + createdFolders + ", skippedFiles=" + skippedFiles
                + ", skippedFolders=" + skippedFolders + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + preloadedFolders.hashCode();
        result = prime * result + listedFolders.hashCode();
        result = prime * result + checkedFiles.hashCode();
        result = prime * result + deletedFiles.hashCode();
        result = prime * result + copiedFiles.hashCode();
        result = prime * result + copiedBytes.hashCode();
        result = prime * result + deletedFolders.hashCode();
        result = prime * result + createdFolders.hashCode();
        result = prime * result + Long.hashCode(skippedFiles.get());
        result = prime * result + Long.hashCode(skippedFolders.get());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Statistics other = (Statistics) obj;
        if (!preloadedFolders.equals(other.preloadedFolders)) return false;
        if (!listedFolders.equals(other.listedFolders)) return false;
        if (!checkedFiles.equals(other.checkedFiles)) return false;
        if (!deletedFiles.equals(other.deletedFiles)) return false;
        if (!copiedFiles.equals(other.copiedFiles)) return false;
        if (!copiedBytes.equals(other.copiedBytes)) return false;
        if (!deletedFolders.equals(other.deletedFolders)) return false;
        if (!createdFolders.equals(other.createdFolders)) return false;
        return skippedFiles.get() == other.skippedFiles.get() && skippedFolders.get() == other.skippedFolders.get();
    }
   
}
