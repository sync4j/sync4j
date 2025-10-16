package com.fathzer.sync4j.sync.parameters;

//TODO Single threaded mode
/**
 * Parameters to control the performance of the synchronizer.
 */
public class PerformanceParameters {
    private boolean fastList;
    private int maxWalkThreads;
    private int maxCopyThreads;
    private int maxComparisonThreads;
    
    /**
     * Creates a new instance of PerformanceParameters.
     * <br>
     * Default values are {@link #maxCopyThreads(int)} = 1 and {@link #maxComparisonThreads(int)} = 1.
     */
    public PerformanceParameters() {
        this.maxWalkThreads = 1;
        this.maxCopyThreads = 0;
        this.maxComparisonThreads = 0;
    }
    
    /**
     * Returns true if the synchronizer should use the fast list mode.
     * @return true if the synchronizer should use the fast list mode
     * @see #fastList(boolean)
     */
    public boolean fastList() {
        return fastList;
    }
    
    /**
     * Sets the fast list mode.
     * <br>In fast list mode, the synchronizer will preload the list of entries of a folder and its subfolders.
     * <br>This can speed up the synchronizer if the list of entries is large, but it also uses more memory and may lead to a OutOfMemoryError
     * if the list of entries is too large.
     * @param fastList true if the synchronizer should use the fast list mode. Default is false.
     * @return this
     */
    public PerformanceParameters fastList(boolean fastList) {
        this.fastList = fastList;
        return this;
    }

    /**
     * Returns the maximum number of threads used to walk folders.
     * @return the maximum number of threads used to walk folders.
     * @see #maxWalkThreads(int)
     */
    public int maxWalkThreads() {
        return maxWalkThreads;
    }
    
    /**
     * Sets the maximum number of threads used to walk folders.
     * @param maxWalkThreads the maximum number of threads used to walk folders. Default is 1.
     * @return this
     */
    public PerformanceParameters maxWalkThreads(int maxWalkThreads) {
        if (maxWalkThreads < 1) {
            throw new IllegalArgumentException("maxWalkThreads must be >= 1");
        }
        this.maxWalkThreads = maxWalkThreads;
        return this;
    }
    
    /**
     * Returns the maximum number of threads used to copy files.
     * @return the maximum number of threads used to copy files
     * @see #maxCopyThreads(int)
     */
    public int maxCopyThreads() {
        return maxCopyThreads;
    }
    
    /**
     * Sets the maximum number of threads used to copy files.
     * @param maxCopyThreads the maximum number of threads used to copy files.
     * Default is 0 (copies are made using the walk threads).
     * @return this
     */
    public PerformanceParameters maxCopyThreads(int maxCopyThreads) {
        if (maxCopyThreads < 0) {
            throw new IllegalArgumentException("maxCopyThreads must be >= 0");
        }
        this.maxCopyThreads = maxCopyThreads;
        return this;
    }
    
    /**
     * Returns the maximum number of threads used to compare files.
     * @return the maximum number of threads used to compare files
     * @see #maxComparisonThreads(int)
     */
    public int maxComparisonThreads() {
        return maxComparisonThreads;
    }
    
    /**
     * Sets the maximum number of threads used to compare files.
     * @param maxComparisonThreads the maximum number of threads used to compare files.
     * Default is 0 (comparisons are made using the walk threads).
     * @return this
     */
    public PerformanceParameters maxComparisonThreads(int maxComparisonThreads) {
        if (maxComparisonThreads < 0) {
            throw new IllegalArgumentException("maxComparisonThreads must be >= 0");
        }
        this.maxComparisonThreads = maxComparisonThreads;
        return this;
    }
}
