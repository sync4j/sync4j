package com.fathzer.sync4j.sync.parameters;

public class PerformanceParameters {
    private boolean fastList;
    private int maxCopyThreads;
    private int maxComparisonThreads;
    
    /**
     * Creates a new instance of PerformanceParameters.
     * <br>
     * Default values are {@link #setMaxTransferThreads(int)} = 1 and {@link #setMaxComparisonThreads(int)} = 1.
     */
    public PerformanceParameters() {
        this.maxCopyThreads = 1;
        this.maxComparisonThreads = 1;
    }
    
    public boolean fastList() {
        return fastList;
    }
    
    public PerformanceParameters fastList(boolean fastList) {
        this.fastList = fastList;
        return this;
    }
    
    public int maxCopyThreads() {
        return maxCopyThreads;
    }
    
    public PerformanceParameters maxCopyThreads(int maxCopyThreads) {
        this.maxCopyThreads = maxCopyThreads;
        return this;
    }
    
    public int maxComparisonThreads() {
        return maxComparisonThreads;
    }
    
    public PerformanceParameters maxComparisonThreads(int maxComparisonThreads) {
        this.maxComparisonThreads = maxComparisonThreads;
        return this;
    }
}
