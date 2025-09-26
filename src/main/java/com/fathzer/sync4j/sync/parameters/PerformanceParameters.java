package com.fathzer.sync4j.sync.parameters;

public class PerformanceParameters {
    private boolean fastList;
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
    
    public boolean isFastList() {
        return fastList;
    }
    
    public void setFastList(boolean fastList) {
        this.fastList = fastList;
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
