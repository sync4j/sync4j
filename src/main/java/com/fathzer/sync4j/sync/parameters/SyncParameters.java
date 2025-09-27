package com.fathzer.sync4j.sync.parameters;

import java.util.List;
import java.util.function.Predicate;

import com.fathzer.sync4j.Entry;

import jakarta.annotation.Nonnull;

public class SyncParameters {
    private boolean dryRun;
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
    public Predicate<Entry> filter() {
        return filter;
    }
    
    public SyncParameters filter(Predicate<Entry> filter) {
        this.filter = filter;
        return this;
    }
    
    @Nonnull
    public FileComparator fileComparator() {
        return fileComparator;
    }
    
    public SyncParameters fileComparator(@Nonnull FileComparator fileComparator) {
        this.fileComparator = fileComparator;
        return this;
    }
    
    @Nonnull
    public PerformanceParameters performance() {
        return performance;
    }
    
    public SyncParameters performance(@Nonnull PerformanceParameters performance) {
        this.performance = performance;
        return this;
    }
    
    public boolean dryRun() {
        return dryRun;
    }
    
    public SyncParameters dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }  
}
