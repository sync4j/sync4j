package com.fathzer.sync4j.parameters;

import java.nio.file.Path;

import java.util.List;
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
    
    /**
     * Creates a new instance of SyncParameters.
     * <br>
     * Default file comparator is {@link FileComparator#SIZE} and {@link FileComparator#MOD_DATE}.
     * Default performance parameters is created by the default constructor of {@link PerformanceParameters}.
     * Default filter is <code>path -> true</code>. 
     * @param source
     * @param destination
     * @throws IllegalArgumentException if the destination is a file and the source is not
     */
    public SyncParameters(@Nonnull File source, @Nonnull File destination) {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.fileComparator = FileComparator.of(List.of(FileComparator.SIZE, FileComparator.MOD_DATE));
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
