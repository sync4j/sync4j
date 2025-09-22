package com.fathzer.sync4j.parameters;

public class PerformanceParameters {
    private int maxTransferThreads;
    private int maxComparisonThreads;
    
    /**
     * Creates a new instance of PerformanceParameters.
     * <br>
     * Default values are {@link #setMaxTransferThreads(int)} = 1 and {@link #setMaxComparisonThreads(int)} = 1.
     */
    public PerformanceParameters() {
        this.maxTransferThreads = 1;
        this.maxComparisonThreads = 1;
    }
    
    public int getMaxTransferThreads() {
        return maxTransferThreads;
    }
    
    public void setMaxTransferThreads(int maxTransferThreads) {
        this.maxTransferThreads = maxTransferThreads;
    }
    
    public int getMaxComparisonThreads() {
        return maxComparisonThreads;
    }
    
    public void setMaxComparisonThreads(int maxComparisonThreads) {
        this.maxComparisonThreads = maxComparisonThreads;
    }
}
