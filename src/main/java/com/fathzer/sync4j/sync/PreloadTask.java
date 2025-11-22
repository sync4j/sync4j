package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.PreloadAction;

class PreLoadTask extends Task<Folder, PreloadAction> {
    PreLoadTask(Context context, PreloadAction action) {
        super(context, action, context.statistics().preloadedFolders());
    }

    @Override
    protected Folder execute() throws IOException {
        return action.folder().preload();
    }

    @Override
    protected Kind kind() {
        return Kind.WALKER;
    }
}
