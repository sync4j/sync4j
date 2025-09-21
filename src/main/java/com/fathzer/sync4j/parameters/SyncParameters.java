package com.fathzer.sync4j.parameters;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

import com.fathzer.sync4j.File;

import jakarta.annotation.Nonnull;

public class SyncParameters {
    private final File source;
    private final File destination;
    private boolean fastList;
    private Predicate<Path> filter;
    private FileComparator fileComparator;
    private PerformanceParameters performance;
    
    public SyncParameters(@Nonnull File source, @Nonnull File destination) {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.fileComparator = (f1, f2) -> f1.getSize() == f2.getSize();
        this.performance = new PerformanceParameters();
        this.fastList = false;
        this.filter = path -> true;
    }

    @Nonnull
    public File getSource() {
        return source;
    }

    @Nonnull
    public File getDestination() {
        return destination;
    }
    
    public boolean isFastList() {
        return fastList;
    }
    
    public void setFastList(boolean fastList) {
        this.fastList = fastList;
    }
    
    @Nonnull
    public Predicate<Path> getFilter() {
        return filter;
    }
    
    public void setFilter(Predicate<Path> filter) {
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
