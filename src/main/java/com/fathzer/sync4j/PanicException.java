package com.fathzer.sync4j;

class PanicException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PanicException(Exception e) {
        super("This should never happen", e);
    }
}
