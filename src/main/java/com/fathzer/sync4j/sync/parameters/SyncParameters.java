package com.fathzer.sync4j.sync.parameters;

import java.util.List;
import java.util.function.Predicate;

import com.fathzer.sync4j.Entry;

import jakarta.annotation.Nonnull;

public class SyncParameters {
    private Predicate<Entry> filter;
    private FileComparator fileComparator;
    private PerformanceParameters performance;
    
    /**
     * Creates a new instance of SyncParameters.
     * <br>
     * Default file comparator is {@link FileComparator#SIZE} and {@link FileComparator#MOD_DATE}.
     * Default performance parameters is created by the default constructor of {@link PerformanceParameters}.
     * Default filter is <code>entry -> true</code>. 
     * @param source
     * @param destination
     * @throws IllegalArgumentException if the destination is a file and the source is not
     */
    public SyncParameters() {
        this.fileComparator = FileComparator.of(List.of(FileComparator.SIZE, FileComparator.MOD_DATE));
        this.performance = new PerformanceParameters();
        this.filter = entry -> true;
    }
   
    @Nonnull
    public Predicate<Entry> getFilter() {
        return filter;
    }
    
    public void setFilter(Predicate<Entry> filter) {
        this.filter = filter;
    }
    
    @Nonnull
    public FileComparator getFileComparator() {
        return fileComparator;
    }
    
    public void setFileComparator(@Nonnull FileComparator fileComparator) {
        this.fileComparator = fileComparator;
    }
    
    @Nonnull
    public PerformanceParameters getPerformance() {
        return performance;
    }
    
    public void setPerformance(@Nonnull PerformanceParameters performance) {
        this.performance = performance;
    }
}
