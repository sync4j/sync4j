package com.fathzer.sync4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

public class Parameters {
    private final File source;
    private final File destination;
    private boolean fastList;
    private Predicate<Path> filter;
    
    public Parameters(File source, File destination) {
        this.source = source;
        this.destination = destination;
    }

    public File getSource() {
        return source;
    }

    public File getDestination() {
        return destination;
    }
    
    public boolean isFastList() {
        return fastList;
    }
    
    public void setFastList(boolean fastList) {
        this.fastList = fastList;
    }
    
    public Predicate<Path> getFilter() {
        return filter;
    }
    
    public void setFilter(Predicate<Path> filter) {
        this.filter = filter;
    }

    public void run() throws IOException {
        
    }
}
