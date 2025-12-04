package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.sync.Event.CompareFileAction;
import com.fathzer.sync4j.sync.IOLambda.IORunnable;

class CompareFileTask extends Task<Boolean, CompareFileAction> {
    private final IORunnable extraAction;

    CompareFileTask(Context context, File source, File destination, IORunnable extraAction) {
        super(context, new CompareFileAction(source, destination), context.statistics().checkedFiles());
        this.extraAction = extraAction;
    }

    @Override
    protected Boolean defaultValue() {
        return true;
    }

    @Override
    protected Boolean execute() throws IOException {
        boolean areSame = context().params().fileComparator().areSame(action().source(), action().destination());
        if (!areSame && extraAction != null) {
            extraAction.run();
        }
        return areSame;
    }

    @Override
    protected Kind kind() {
        return Kind.CHECKER;
    }
}
