package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

public interface Folder extends Entry {
    /**
     * List the direct children of this file.
     * @return the list of children
     * @throws IOException if an I/O error occurs or if this file is not a folder
     */
    List<Entry> list() throws IOException;
}
