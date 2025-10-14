package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.PreloadAction;

class PreLoadTask extends Task<Folder, PreloadAction> {
    private final ExecutorService executorService;
    
    PreLoadTask(Context context, PreloadAction action, ExecutorService executorService) {
        super(context, action, context.statistics().preloadedFolders());
        this.executorService = executorService;
    }

    @Override
    protected boolean skipOnDryRun() {
        return false;
    }

    @Override
    protected Folder execute() throws IOException {
        return action.folder().preload();
    }

    @Override
    protected ExecutorService executorService() {
        return executorService;
    }
}
