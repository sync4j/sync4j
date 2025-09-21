package com.fathzer.sync4j.parameters;

public class PerformanceParameters {
    private int maxTransferThreads;
    private int maxComparisonThreads;
    
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
