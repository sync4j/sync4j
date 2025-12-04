package com.fathzer.sync4j.sync;

import java.io.IOException;

final class IOLambda {
    private IOLambda() {}
    
    @FunctionalInterface
    public interface IORunnable {
        void run() throws IOException;
    }
}
