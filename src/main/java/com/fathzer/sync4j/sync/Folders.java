package com.fathzer.sync4j.sync;

import com.fathzer.sync4j.Folder;

class Folders {
    Folder source;
    Folder destination;

    Folders(Folder source, Folder destination) {
        this.source = source;
        this.destination = destination;
    }
}