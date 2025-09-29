package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.sync.Event.CompareFileAction;

public class CompareFileTask extends Task<Boolean, CompareFileAction> {
    CompareFileTask(Context context, File source, File destination) {
        super(context, new CompareFileAction(source, destination), context.statistics().checkedFiles());
    }

    @Override
    protected Boolean getDefaultValue() {
        return true;
    }

    @Override
    protected Boolean execute() throws IOException {
        return context().params().fileComparator().areSame(action.source(), action.destination());
    }

    @Override
    protected boolean skipOnDryRun() {
        return false;
    }

    @Override
    protected ExecutorService executorService() {
        return context().checkService;
    }
}
