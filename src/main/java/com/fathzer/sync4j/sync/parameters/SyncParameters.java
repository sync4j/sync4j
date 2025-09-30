package com.fathzer.sync4j.sync.parameters;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.sync.Event;

import jakarta.annotation.Nonnull;

public class SyncParameters {
    private boolean dryRun;
    private Predicate<Entry> filter;
    private FileComparator fileComparator;
    private PerformanceParameters performance;
    private Consumer<Event> eventListener;
    
    /**
     * Creates a new instance of SyncParameters.
     * <br>
     * Default file comparator is {@link FileComparator#SIZE} and {@link FileComparator#MOD_DATE}.
     * Default performance parameters is created by the default constructor of {@link PerformanceParameters}.
     * Default filter is <code>entry -> true</code>. 
     */
    public SyncParameters() {
        this.fileComparator = FileComparator.of(List.of(FileComparator.SIZE, FileComparator.MOD_DATE));
        this.performance = new PerformanceParameters();
        this.filter = entry -> true;
        this.eventListener = event -> {};
    }
   
    @Nonnull
    public Predicate<Entry> filter() {
        return filter;
    }
    
    @Nonnull
    public SyncParameters filter(@Nonnull Predicate<Entry> filter) {
        this.filter = Objects.requireNonNull(filter);
        return this;
    }
    
    @Nonnull
    public FileComparator fileComparator() {
        return fileComparator;
    }
    
    @Nonnull
    public SyncParameters fileComparator(@Nonnull FileComparator fileComparator) {
        this.fileComparator = Objects.requireNonNull(fileComparator);
        return this;
    }
    
    @Nonnull
    public PerformanceParameters performance() {
        return performance;
    }
    
    public boolean dryRun() {
        return dryRun;
    }
    
    @Nonnull
    public SyncParameters dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }
    
    @Nonnull
    public Consumer<Event> eventListener() {
        return eventListener;
    }
    
    @Nonnull
    public SyncParameters eventListener(@Nonnull Consumer<Event> eventListener) {
        this.eventListener = Objects.requireNonNull(eventListener);
        return this;
    }
}
