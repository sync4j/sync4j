package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.sync.Event.CompareFileAction;

class CompareFileTask extends Task<Boolean, CompareFileAction> {
    CompareFileTask(Context context, File source, File destination) {
        super(context, new CompareFileAction(source, destination), context.statistics().checkedFiles());
    }

    @Override
    protected Boolean defaultValue() {
        return true;
    }

    @Override
    protected Boolean execute() throws IOException {
        return context.params().fileComparator().areSame(action.source(), action.destination());
    }

    @Override
    protected Kind kind() {
        return Kind.CHECKER;
    }
}
